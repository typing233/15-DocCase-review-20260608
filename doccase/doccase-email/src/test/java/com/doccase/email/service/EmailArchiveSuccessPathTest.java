package com.doccase.email.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.doccase.common.dto.DocumentDTO;
import com.doccase.common.dto.EmailArchiveEvent;
import com.doccase.common.response.ApiResponse;
import com.doccase.common.util.DistributedLockUtil;
import com.doccase.email.domain.entity.EmailAccount;
import com.doccase.email.domain.entity.EmailArchiveRecord;
import com.doccase.email.feign.DocumentCreateDTO;
import com.doccase.email.feign.DocumentServiceClient;
import com.doccase.email.mapper.EmailAccountMapper;
import com.doccase.email.mapper.EmailArchiveRecordMapper;
import com.doccase.email.metrics.EmailMetrics;
import com.doccase.email.service.impl.EmailPollingServiceImpl;
import io.minio.MinioClient;
import io.minio.ObjectWriteResponse;
import io.minio.PutObjectArgs;
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Email Archiving - Success Path Integration (Mocked IMAP)")
class EmailArchiveSuccessPathTest {

    @Mock private EmailAccountMapper accountMapper;
    @Mock private EmailArchiveRecordMapper archiveRecordMapper;
    @Mock private AuditService auditService;
    @Mock private DistributedLockUtil distributedLockUtil;
    @Mock private RabbitTemplate rabbitTemplate;
    @Mock private EmailMetrics emailMetrics;
    @Mock private MinioClient minioClient;
    @Mock private DocumentServiceClient documentServiceClient;

    @InjectMocks
    private EmailPollingServiceImpl pollingService;

    @Captor private ArgumentCaptor<EmailArchiveRecord> recordCaptor;
    @Captor private ArgumentCaptor<MultipartFile> fileCaptor;
    @Captor private ArgumentCaptor<DocumentCreateDTO> dtoCaptor;

    private EmailAccount testAccount;

    @BeforeEach
    void setup() {
        ReflectionTestUtils.setField(pollingService, "maxMessagesPerPoll", 100);
        ReflectionTestUtils.setField(pollingService, "connectionTimeout", 30000);
        ReflectionTestUtils.setField(pollingService, "readTimeout", 60000);
        ReflectionTestUtils.setField(pollingService, "maxAttachmentSizeMb", 100);
        ReflectionTestUtils.setField(pollingService, "allowedExtensions", "pdf,doc,docx,xls,xlsx,png,zip");
        ReflectionTestUtils.setField(pollingService, "aesKey", "doccase-email-aes256-key-32char!");
        ReflectionTestUtils.setField(pollingService, "minioBucket", "test-bucket");

        testAccount = new EmailAccount();
        testAccount.setId(1L);
        testAccount.setUserId(100L);
        testAccount.setTenantId("tenant-A");
        testAccount.setEmailAddress("user@corp.com");
        testAccount.setImapHost("imap.corp.com");
        testAccount.setImapPort(993);
        testAccount.setUseSsl(true);
        testAccount.setUsername("user@corp.com");
        testAccount.setPasswordEncrypted(pollingService.encryptPassword("secret"));
        testAccount.setFolderFilter("INBOX");
        testAccount.setIsEnabled(true);
        testAccount.setStatus(1);
        testAccount.setLastPollUid(100L);
    }

    @Test
    @DisplayName("Full success: poll -> fetch UID messages -> archive attachment -> create document -> update checkpoint")
    void fullSuccessPath_pollWithNewMessage() throws Exception {
        // Arrange: mock IMAP chain
        Store mockStore = mock(Store.class);
        when(mockStore.isConnected()).thenReturn(true);

        // Create a folder that implements UIDFolder
        TestUIDFolder mockFolder = mock(TestUIDFolder.class);
        when(mockStore.getFolder("INBOX")).thenReturn(mockFolder);
        when(mockFolder.exists()).thenReturn(true);
        when(mockFolder.getUIDValidity()).thenReturn(12345L);

        // Create a message with attachment at UID 101
        MimeMessage mockMessage = mock(MimeMessage.class);
        when(mockMessage.getMessageID()).thenReturn("<msg-101@corp.com>");
        when(mockMessage.getFrom()).thenReturn(new InternetAddress[]{new InternetAddress("sender@corp.com")});
        when(mockMessage.getSubject()).thenReturn("Invoice Q4");
        when(mockMessage.getReceivedDate()).thenReturn(new Date());

        // Multipart with one attachment
        Multipart mockMultipart = mock(Multipart.class);
        when(mockMessage.getContent()).thenReturn(mockMultipart);
        when(mockMultipart.getCount()).thenReturn(1);

        BodyPart mockBodyPart = mock(BodyPart.class);
        when(mockMultipart.getBodyPart(0)).thenReturn(mockBodyPart);
        when(mockBodyPart.getDisposition()).thenReturn(Part.ATTACHMENT);
        when(mockBodyPart.getFileName()).thenReturn("invoice.pdf");
        when(mockBodyPart.getSize()).thenReturn(1024);
        when(mockBodyPart.getContentType()).thenReturn("application/pdf");

        byte[] pdfContent = "fake-pdf-content-not-encrypted".getBytes();
        when(mockBodyPart.getInputStream()).thenReturn(new ByteArrayInputStream(pdfContent));

        // UIDFolder returns our message for UID > 100
        Message[] messages = new Message[]{mockMessage};
        when(mockFolder.getMessagesByUID(101L, UIDFolder.LASTUID)).thenReturn(messages);
        when(mockFolder.getUID(mockMessage)).thenReturn(101L);

        // Dedup check: no existing record
        when(archiveRecordMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

        // MinIO upload succeeds
        when(minioClient.putObject(any(PutObjectArgs.class))).thenReturn(mock(ObjectWriteResponse.class));

        // Document service returns success with ID
        DocumentDTO createdDoc = new DocumentDTO();
        createdDoc.setId(999L);
        ApiResponse<DocumentDTO> docResponse = ApiResponse.success(createdDoc);
        when(documentServiceClient.createDocument(eq(100L), any(MultipartFile.class), any(DocumentCreateDTO.class)))
                .thenReturn(docResponse);

        // Distributed lock executes immediately
        doAnswer(invocation -> {
            Runnable runnable = invocation.getArgument(4);
            runnable.run();
            return null;
        }).when(distributedLockUtil).executeWithLock(anyString(), anyLong(), anyLong(), any(TimeUnit.class), any(Runnable.class));

        // Use reflection to inject the mocked Store via a spy on connectToImap
        EmailPollingServiceImpl spyService = spy(pollingService);
        doReturn(mockStore).when(spyService).connectToImapInternal(any(EmailAccount.class));

        // Act
        spyService.pollAccount(testAccount);

        // Assert: archive record was inserted with status=1 (archived) and documentId=999
        verify(archiveRecordMapper).insert(recordCaptor.capture());
        EmailArchiveRecord savedRecord = recordCaptor.getValue();
        assertEquals(1, savedRecord.getStatus());
        assertEquals(999L, savedRecord.getDocumentId());
        assertEquals("invoice.pdf", savedRecord.getAttachmentFileName());
        assertEquals("<msg-101@corp.com>", savedRecord.getMessageId());
        assertEquals(101L, savedRecord.getMessageUid());
        assertEquals("tenant-A", savedRecord.getTenantId());
        assertFalse(savedRecord.getIsEncrypted());

        // Assert: document service was called with correct params
        verify(documentServiceClient).createDocument(eq(100L), fileCaptor.capture(), dtoCaptor.capture());
        assertEquals("invoice.pdf", fileCaptor.getValue().getOriginalFilename());
        assertEquals("application/pdf", fileCaptor.getValue().getContentType());
        assertEquals("invoice.pdf", dtoCaptor.getValue().getTitle());

        // Assert: MinIO upload happened
        verify(minioClient).putObject(any(PutObjectArgs.class));

        // Assert: MQ event published
        verify(rabbitTemplate).convertAndSend(anyString(), anyString(), any(EmailArchiveEvent.class));

        // Assert: metrics recorded
        verify(emailMetrics).recordArchived();
        verify(emailMetrics).recordPoll(eq(1L), eq(1));

        // Assert: checkpoint updated to 101
        verify(accountMapper).updateById(argThat(acc ->
                acc.getLastPollUid() == 101L && acc.getStatus() == 1));

        // Assert: audit logs written
        verify(auditService).log(eq(1L), eq("POLL_START"), isNull(), isNull(), contains("UID-based"));
        verify(auditService).log(eq(1L), eq("ARCHIVE"), eq("<msg-101@corp.com>"),
                eq("invoice.pdf"), contains("uid=101"));
        verify(auditService).log(eq(1L), eq("POLL_END"), isNull(), isNull(), contains("checkpoint UID=101"));
    }

    @Test
    @DisplayName("Dedup: duplicate attachment is skipped, metrics recorded")
    void dedup_duplicateAttachment_skipped() throws Exception {
        Store mockStore = mock(Store.class);
        when(mockStore.isConnected()).thenReturn(true);

        TestUIDFolder mockFolder = mock(TestUIDFolder.class);
        when(mockStore.getFolder("INBOX")).thenReturn(mockFolder);
        when(mockFolder.exists()).thenReturn(true);
        when(mockFolder.getUIDValidity()).thenReturn(12345L);

        MimeMessage mockMessage = mock(MimeMessage.class);
        when(mockMessage.getMessageID()).thenReturn("<dup@corp.com>");
        when(mockMessage.getFrom()).thenReturn(new InternetAddress[]{new InternetAddress("s@c.com")});
        when(mockMessage.getSubject()).thenReturn("Dup");
        when(mockMessage.getReceivedDate()).thenReturn(new Date());

        Multipart mockMultipart = mock(Multipart.class);
        when(mockMessage.getContent()).thenReturn(mockMultipart);
        when(mockMultipart.getCount()).thenReturn(1);

        BodyPart mockBodyPart = mock(BodyPart.class);
        when(mockMultipart.getBodyPart(0)).thenReturn(mockBodyPart);
        when(mockBodyPart.getDisposition()).thenReturn(Part.ATTACHMENT);
        when(mockBodyPart.getFileName()).thenReturn("file.pdf");
        when(mockBodyPart.getSize()).thenReturn(100);
        when(mockBodyPart.getInputStream()).thenReturn(new ByteArrayInputStream("dup".getBytes()));

        Message[] messages = new Message[]{mockMessage};
        when(mockFolder.getMessagesByUID(101L, UIDFolder.LASTUID)).thenReturn(messages);
        when(mockFolder.getUID(mockMessage)).thenReturn(101L);

        // Dedup check: record already exists
        when(archiveRecordMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        doAnswer(invocation -> {
            Runnable runnable = invocation.getArgument(4);
            runnable.run();
            return null;
        }).when(distributedLockUtil).executeWithLock(anyString(), anyLong(), anyLong(), any(TimeUnit.class), any(Runnable.class));

        EmailPollingServiceImpl spyService = spy(pollingService);
        doReturn(mockStore).when(spyService).connectToImapInternal(any(EmailAccount.class));

        spyService.pollAccount(testAccount);

        // Should NOT insert a new record
        verify(archiveRecordMapper, never()).insert(any());
        // Should record duplicate metric
        verify(emailMetrics).recordDuplicate();
        // Should NOT call document service
        verify(documentServiceClient, never()).createDocument(anyLong(), any(), any());
    }

    @Test
    @DisplayName("Encrypted attachment: detected and skipped with status=2")
    void encrypted_attachment_skipped() throws Exception {
        Store mockStore = mock(Store.class);
        when(mockStore.isConnected()).thenReturn(true);

        TestUIDFolder mockFolder = mock(TestUIDFolder.class);
        when(mockStore.getFolder("INBOX")).thenReturn(mockFolder);
        when(mockFolder.exists()).thenReturn(true);
        when(mockFolder.getUIDValidity()).thenReturn(12345L);

        MimeMessage mockMessage = mock(MimeMessage.class);
        when(mockMessage.getMessageID()).thenReturn("<enc@corp.com>");
        when(mockMessage.getFrom()).thenReturn(new InternetAddress[]{new InternetAddress("s@c.com")});
        when(mockMessage.getSubject()).thenReturn("Encrypted");
        when(mockMessage.getReceivedDate()).thenReturn(new Date());

        Multipart mockMultipart = mock(Multipart.class);
        when(mockMessage.getContent()).thenReturn(mockMultipart);
        when(mockMultipart.getCount()).thenReturn(1);

        BodyPart mockBodyPart = mock(BodyPart.class);
        when(mockMultipart.getBodyPart(0)).thenReturn(mockBodyPart);
        when(mockBodyPart.getDisposition()).thenReturn(Part.ATTACHMENT);
        when(mockBodyPart.getFileName()).thenReturn("secret.zip");
        when(mockBodyPart.getSize()).thenReturn(1000);
        when(mockBodyPart.getContentType()).thenReturn("application/zip");

        // Create content with ZIP encryption flag set (byte[6] bit 0 = 1)
        byte[] encryptedZip = new byte[100];
        encryptedZip[0] = 'P'; encryptedZip[1] = 'K'; // PK magic
        encryptedZip[6] = 0x01; // encryption flag
        when(mockBodyPart.getInputStream()).thenReturn(new ByteArrayInputStream(encryptedZip));

        Message[] messages = new Message[]{mockMessage};
        when(mockFolder.getMessagesByUID(101L, UIDFolder.LASTUID)).thenReturn(messages);
        when(mockFolder.getUID(mockMessage)).thenReturn(101L);

        when(archiveRecordMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

        doAnswer(invocation -> {
            Runnable runnable = invocation.getArgument(4);
            runnable.run();
            return null;
        }).when(distributedLockUtil).executeWithLock(anyString(), anyLong(), anyLong(), any(TimeUnit.class), any(Runnable.class));

        EmailPollingServiceImpl spyService = spy(pollingService);
        doReturn(mockStore).when(spyService).connectToImapInternal(any(EmailAccount.class));

        spyService.pollAccount(testAccount);

        // Record saved with status=2 (skipped) and encrypted flag
        verify(archiveRecordMapper).insert(recordCaptor.capture());
        EmailArchiveRecord saved = recordCaptor.getValue();
        assertEquals(2, saved.getStatus());
        assertTrue(saved.getIsEncrypted());
        assertEquals("encrypted attachment", saved.getSkipReason());

        verify(emailMetrics).recordEncrypted();
        verify(documentServiceClient, never()).createDocument(anyLong(), any(), any());
    }

    @Test
    @DisplayName("Document creation fails: record saved with status=3, error message retained")
    void documentCreation_fails_statusIsError() throws Exception {
        Store mockStore = mock(Store.class);
        when(mockStore.isConnected()).thenReturn(true);

        TestUIDFolder mockFolder = mock(TestUIDFolder.class);
        when(mockStore.getFolder("INBOX")).thenReturn(mockFolder);
        when(mockFolder.exists()).thenReturn(true);
        when(mockFolder.getUIDValidity()).thenReturn(12345L);

        MimeMessage mockMessage = mock(MimeMessage.class);
        when(mockMessage.getMessageID()).thenReturn("<fail@corp.com>");
        when(mockMessage.getFrom()).thenReturn(new InternetAddress[]{new InternetAddress("s@c.com")});
        when(mockMessage.getSubject()).thenReturn("Fail");
        when(mockMessage.getReceivedDate()).thenReturn(new Date());

        Multipart mockMultipart = mock(Multipart.class);
        when(mockMessage.getContent()).thenReturn(mockMultipart);
        when(mockMultipart.getCount()).thenReturn(1);

        BodyPart mockBodyPart = mock(BodyPart.class);
        when(mockMultipart.getBodyPart(0)).thenReturn(mockBodyPart);
        when(mockBodyPart.getDisposition()).thenReturn(Part.ATTACHMENT);
        when(mockBodyPart.getFileName()).thenReturn("report.docx");
        when(mockBodyPart.getSize()).thenReturn(2048);
        when(mockBodyPart.getContentType()).thenReturn("application/vnd.openxmlformats");
        when(mockBodyPart.getInputStream()).thenReturn(new ByteArrayInputStream("word-doc".getBytes()));

        Message[] messages = new Message[]{mockMessage};
        when(mockFolder.getMessagesByUID(101L, UIDFolder.LASTUID)).thenReturn(messages);
        when(mockFolder.getUID(mockMessage)).thenReturn(101L);

        when(archiveRecordMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(minioClient.putObject(any(PutObjectArgs.class))).thenReturn(mock(ObjectWriteResponse.class));

        // Document service throws
        when(documentServiceClient.createDocument(anyLong(), any(MultipartFile.class), any(DocumentCreateDTO.class)))
                .thenThrow(new RuntimeException("Service unavailable"));

        doAnswer(invocation -> {
            Runnable runnable = invocation.getArgument(4);
            runnable.run();
            return null;
        }).when(distributedLockUtil).executeWithLock(anyString(), anyLong(), anyLong(), any(TimeUnit.class), any(Runnable.class));

        EmailPollingServiceImpl spyService = spy(pollingService);
        doReturn(mockStore).when(spyService).connectToImapInternal(any(EmailAccount.class));

        spyService.pollAccount(testAccount);

        verify(archiveRecordMapper).insert(recordCaptor.capture());
        EmailArchiveRecord saved = recordCaptor.getValue();
        assertEquals(3, saved.getStatus());
        assertNull(saved.getDocumentId());
        assertTrue(saved.getErrorMessage().contains("Document creation failed"));
    }

    /**
     * Abstract class combining Folder + UIDFolder for mocking.
     * Mockito can mock this because both are needed in the test.
     */
    static abstract class TestUIDFolder extends Folder implements UIDFolder {
        public TestUIDFolder(Store store) { super(store); }
    }
}
