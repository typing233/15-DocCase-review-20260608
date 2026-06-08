package com.doccase.email.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.doccase.common.constant.MqConstants;
import com.doccase.common.constant.RedisKeyConstants;
import com.doccase.common.dto.EmailArchiveEvent;
import com.doccase.common.util.DistributedLockUtil;
import com.doccase.email.domain.entity.EmailAccount;
import com.doccase.email.domain.entity.EmailArchiveRecord;
import com.doccase.email.feign.DocumentCreateDTO;
import com.doccase.email.feign.DocumentServiceClient;
import com.doccase.email.mapper.EmailAccountMapper;
import com.doccase.email.mapper.EmailArchiveRecordMapper;
import com.doccase.email.metrics.EmailMetrics;
import com.doccase.email.service.AuditService;
import com.doccase.email.service.EmailPollingService;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import jakarta.mail.*;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailPollingServiceImpl implements EmailPollingService {

    private final EmailAccountMapper accountMapper;
    private final EmailArchiveRecordMapper archiveRecordMapper;
    private final AuditService auditService;
    private final DistributedLockUtil distributedLockUtil;
    private final RabbitTemplate rabbitTemplate;
    private final EmailMetrics emailMetrics;
    private final MinioClient minioClient;
    private final DocumentServiceClient documentServiceClient;

    @Value("${email.poll.max-messages-per-poll:100}")
    private int maxMessagesPerPoll;

    @Value("${email.poll.connection-timeout-ms:30000}")
    private int connectionTimeout;

    @Value("${email.poll.read-timeout-ms:60000}")
    private int readTimeout;

    @Value("${email.archive.max-attachment-size-mb:100}")
    private int maxAttachmentSizeMb;

    @Value("${email.archive.allowed-extensions:pdf,doc,docx,xls,xlsx,ppt,pptx,jpg,png,zip,rar}")
    private String allowedExtensions;

    @Value("${email.encryption.aes-key:doccase-email-aes256-key-32char!}")
    private String aesKey;

    @Value("${minio.bucket:doccase-email-attachments}")
    private String minioBucket;

    @Override
    public void pollAllAccounts() {
        LambdaQueryWrapper<EmailAccount> query = new LambdaQueryWrapper<>();
        query.eq(EmailAccount::getIsEnabled, true)
                .eq(EmailAccount::getStatus, 1);
        List<EmailAccount> accounts = accountMapper.selectList(query);

        for (EmailAccount account : accounts) {
            try {
                pollAccount(account);
            } catch (Exception e) {
                log.error("Failed to poll account {}: {}", account.getEmailAddress(), e.getMessage());
                account.setStatus(0);
                account.setErrorMessage(e.getMessage());
                accountMapper.updateById(account);
            }
        }
    }

    @Override
    @Retryable(retryFor = MessagingException.class, maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2))
    public void pollAccount(EmailAccount account) {
        String lockKey = RedisKeyConstants.EMAIL_POLL_LOCK_PREFIX + account.getId();
        distributedLockUtil.executeWithLock(lockKey, 5, 300, TimeUnit.SECONDS, () -> {
            doPoll(account);
        });
    }

    @Override
    public void retryRecord(EmailArchiveRecord record) {
        EmailAccount account = accountMapper.selectById(record.getAccountId());
        if (account == null) {
            record.setStatus(3);
            record.setErrorMessage("Account not found");
            record.setUpdatedAt(LocalDateTime.now());
            archiveRecordMapper.updateById(record);
            return;
        }

        String lockKey = RedisKeyConstants.EMAIL_POLL_LOCK_PREFIX + account.getId() + ":retry:" + record.getId();
        distributedLockUtil.executeWithLock(lockKey, 5, 120, TimeUnit.SECONDS, () -> {
            doRetryRecord(account, record);
        });
    }

    private void doRetryRecord(EmailAccount account, EmailArchiveRecord record) {
        auditService.log(account.getId(), "RETRY_START", record.getMessageId(),
                record.getAttachmentFileName(), "retry #" + record.getRetryCount());

        Store store = null;
        try {
            store = connectToImapInternal(account);
            String[] folders = account.getFolderFilter() != null ?
                    account.getFolderFilter().split(",") : new String[]{"INBOX"};

            boolean found = false;
            for (String folderName : folders) {
                Folder folder = store.getFolder(folderName.trim());
                if (!folder.exists()) continue;

                if (folder instanceof UIDFolder uidFolder) {
                    folder.open(Folder.READ_ONLY);
                    if (record.getMessageUid() != null && record.getMessageUid() > 0) {
                        Message msg = uidFolder.getMessageByUID(record.getMessageUid());
                        if (msg instanceof MimeMessage mimeMessage) {
                            found = retryAttachmentFromMessage(account, record, mimeMessage);
                        }
                    }
                    folder.close(false);
                }
                if (found) break;
            }

            if (!found) {
                record.setStatus(3);
                record.setErrorMessage("Original message not found in mailbox");
                record.setUpdatedAt(LocalDateTime.now());
                archiveRecordMapper.updateById(record);
                auditService.log(account.getId(), "RETRY_FAILED", record.getMessageId(),
                        record.getAttachmentFileName(), "message not found");
            }
        } catch (Exception e) {
            log.error("Retry failed for record {}", record.getId(), e);
            record.setStatus(3);
            record.setErrorMessage(e.getMessage());
            record.setRetryCount(record.getRetryCount() + 1);
            record.setUpdatedAt(LocalDateTime.now());
            archiveRecordMapper.updateById(record);
            auditService.log(account.getId(), "RETRY_FAILED", record.getMessageId(),
                    record.getAttachmentFileName(), e.getMessage());
        } finally {
            if (store != null && store.isConnected()) {
                try { store.close(); } catch (Exception ignored) {}
            }
        }
    }

    private boolean retryAttachmentFromMessage(EmailAccount account, EmailArchiveRecord record,
                                               MimeMessage message) throws Exception {
        Object content = message.getContent();
        if (!(content instanceof Multipart multipart)) return false;

        for (int i = 0; i < multipart.getCount(); i++) {
            BodyPart bodyPart = multipart.getBodyPart(i);
            String fileName = bodyPart.getFileName();
            if (fileName == null) continue;
            if (!fileName.equals(record.getAttachmentFileName())) continue;

            byte[] attachmentBytes = readAttachment(bodyPart);
            String hash = sha256(attachmentBytes);
            if (!hash.equals(record.getAttachmentHash())) continue;

            // Re-attempt document creation
            Long documentId = uploadAndCreateDocument(account, record.getAttachmentFileName(),
                    attachmentBytes, record.getAttachmentMimeType());

            record.setDocumentId(documentId);
            record.setStatus(1);
            record.setErrorMessage(null);
            record.setUpdatedAt(LocalDateTime.now());
            archiveRecordMapper.updateById(record);

            auditService.log(account.getId(), "RETRY_SUCCESS", record.getMessageId(),
                    record.getAttachmentFileName(), "documentId=" + documentId);
            return true;
        }
        return false;
    }

    private void doPoll(EmailAccount account) {
        auditService.log(account.getId(), "POLL_START", null, null,
                "Starting UID-based incremental poll for " + account.getEmailAddress());

        Store store = null;
        try {
            store = connectToImapInternal(account);
            String[] folders = account.getFolderFilter() != null ?
                    account.getFolderFilter().split(",") : new String[]{"INBOX"};

            int totalProcessed = 0;
            long maxUidSeen = account.getLastPollUid() != null ? account.getLastPollUid() : 0L;

            for (String folderName : folders) {
                Folder folder = store.getFolder(folderName.trim());
                if (!folder.exists()) {
                    log.warn("Folder {} does not exist for account {}", folderName, account.getEmailAddress());
                    continue;
                }
                folder.open(Folder.READ_ONLY);

                if (folder instanceof UIDFolder uidFolder) {
                    long startUid = maxUidSeen + 1;
                    long uidValidity = uidFolder.getUIDValidity();

                    Message[] messages = uidFolder.getMessagesByUID(startUid, UIDFolder.LASTUID);
                    if (messages == null) messages = new Message[0];

                    for (int i = 0; i < messages.length && totalProcessed < maxMessagesPerPoll; i++) {
                        Message message = messages[i];
                        if (message == null) continue;

                        long uid = uidFolder.getUID(message);
                        if (uid <= maxUidSeen) continue;

                        if (message instanceof MimeMessage mimeMessage) {
                            processMessage(account, mimeMessage, uid);
                            totalProcessed++;
                        }

                        if (uid > maxUidSeen) {
                            maxUidSeen = uid;
                        }
                    }
                } else {
                    // Fallback for non-UID folders: process last N messages
                    Message[] messages = folder.getMessages();
                    int startIdx = Math.max(0, messages.length - maxMessagesPerPoll);
                    for (int i = startIdx; i < messages.length && totalProcessed < maxMessagesPerPoll; i++) {
                        if (messages[i] instanceof MimeMessage mimeMessage) {
                            processMessage(account, mimeMessage, 0L);
                            totalProcessed++;
                        }
                    }
                }

                folder.close(false);
            }

            // Persist checkpoint
            account.setLastPollUid(maxUidSeen);
            account.setLastPollAt(LocalDateTime.now());
            account.setStatus(1);
            account.setErrorMessage(null);
            accountMapper.updateById(account);

            emailMetrics.recordPoll(account.getId(), totalProcessed);
            auditService.log(account.getId(), "POLL_END", null, null,
                    "Processed " + totalProcessed + " messages, checkpoint UID=" + maxUidSeen);

        } catch (Exception e) {
            log.error("IMAP poll failed for account {}", account.getEmailAddress(), e);
            account.setStatus(0);
            account.setErrorMessage(e.getMessage());
            accountMapper.updateById(account);
            emailMetrics.recordPollError(account.getId());
            auditService.log(account.getId(), "ERROR", null, null, e.getMessage());
            throw new RuntimeException("IMAP poll failed", e);
        } finally {
            if (store != null && store.isConnected()) {
                try { store.close(); } catch (Exception ignored) {}
            }
        }
    }

    private void processMessage(EmailAccount account, MimeMessage message, long uid) throws Exception {
        String messageId = message.getMessageID();
        if (messageId == null) messageId = UUID.randomUUID().toString();

        String from = message.getFrom() != null && message.getFrom().length > 0 ?
                message.getFrom()[0].toString() : "unknown";
        String subject = message.getSubject();
        Date receivedDate = message.getReceivedDate();

        Object content = message.getContent();
        if (content instanceof Multipart multipart) {
            for (int i = 0; i < multipart.getCount(); i++) {
                BodyPart bodyPart = multipart.getBodyPart(i);
                if (Part.ATTACHMENT.equalsIgnoreCase(bodyPart.getDisposition()) ||
                        (bodyPart.getFileName() != null && !bodyPart.getFileName().isEmpty())) {

                    String fileName = bodyPart.getFileName();
                    if (fileName == null) continue;

                    if (!isAllowedExtension(fileName)) {
                        auditService.log(account.getId(), "SKIP", messageId, fileName, "extension not allowed");
                        continue;
                    }

                    int size = bodyPart.getSize();
                    if (size > maxAttachmentSizeMb * 1024 * 1024) {
                        auditService.log(account.getId(), "SKIP", messageId, fileName, "exceeds max size");
                        continue;
                    }

                    processAttachment(account, messageId, uid, from, subject, receivedDate, bodyPart, fileName);
                }
            }
        }
    }

    private void processAttachment(EmailAccount account, String messageId, long uid,
                                   String from, String subject, Date receivedDate,
                                   BodyPart bodyPart, String fileName) {
        try {
            byte[] attachmentBytes = readAttachment(bodyPart);
            String hash = sha256(attachmentBytes);

            // Idempotent check
            LambdaQueryWrapper<EmailArchiveRecord> dedupQuery = new LambdaQueryWrapper<>();
            dedupQuery.eq(EmailArchiveRecord::getAccountId, account.getId())
                    .eq(EmailArchiveRecord::getMessageId, messageId)
                    .eq(EmailArchiveRecord::getAttachmentHash, hash);
            if (archiveRecordMapper.selectCount(dedupQuery) > 0) {
                auditService.log(account.getId(), "SKIP", messageId, fileName, "duplicate");
                emailMetrics.recordDuplicate();
                return;
            }

            boolean isEncrypted = detectEncrypted(fileName, attachmentBytes);

            EmailArchiveRecord record = new EmailArchiveRecord();
            record.setAccountId(account.getId());
            record.setTenantId(account.getTenantId());
            record.setMessageId(messageId);
            record.setMessageUid(uid);
            record.setFromAddress(from);
            record.setSubject(subject);
            record.setReceivedAt(receivedDate != null ?
                    receivedDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime() : null);
            record.setAttachmentFileName(fileName);
            record.setAttachmentHash(hash);
            record.setAttachmentSize((long) attachmentBytes.length);
            record.setAttachmentMimeType(bodyPart.getContentType());
            record.setIsEncrypted(isEncrypted);
            record.setDecryptionAttempted(false);
            record.setRetryCount(0);
            record.setCreatedAt(LocalDateTime.now());
            record.setUpdatedAt(LocalDateTime.now());

            if (isEncrypted) {
                record.setStatus(2);
                record.setSkipReason("encrypted attachment");
                archiveRecordMapper.insert(record);
                auditService.log(account.getId(), "SKIP", messageId, fileName, "encrypted");
                emailMetrics.recordEncrypted();
                return;
            }

            // Upload to MinIO for durable storage
            String objectKey = account.getTenantId() + "/" + account.getId() + "/" + hash + "/" + fileName;
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(minioBucket)
                    .object(objectKey)
                    .stream(new ByteArrayInputStream(attachmentBytes), attachmentBytes.length, -1)
                    .contentType(bodyPart.getContentType())
                    .build());

            // Create document via document service
            Long documentId = null;
            try {
                documentId = uploadAndCreateDocument(account, fileName, attachmentBytes, bodyPart.getContentType());
                record.setStatus(1);
                record.setDocumentId(documentId);
            } catch (Exception e) {
                log.warn("Failed to create document for attachment {}, will queue for retry", fileName, e);
                record.setStatus(3);
                record.setErrorMessage("Document creation failed: " + e.getMessage());
            }

            archiveRecordMapper.insert(record);

            // Publish event
            EmailArchiveEvent event = EmailArchiveEvent.builder()
                    .accountId(account.getId())
                    .tenantId(account.getTenantId())
                    .messageId(messageId)
                    .fromAddress(from)
                    .subject(subject)
                    .attachmentFileName(fileName)
                    .attachmentHash(hash)
                    .attachmentSize((long) attachmentBytes.length)
                    .attachmentMimeType(bodyPart.getContentType())
                    .archivedAt(LocalDateTime.now())
                    .build();
            rabbitTemplate.convertAndSend(MqConstants.EXCHANGE_EMAIL, MqConstants.RK_EMAIL_ATTACHMENT_ARCHIVED, event);

            auditService.log(account.getId(), "ARCHIVE", messageId, fileName,
                    "uid=" + uid + ", size=" + attachmentBytes.length + ", documentId=" + documentId);
            emailMetrics.recordArchived();

        } catch (Exception e) {
            log.error("Failed to process attachment {} from message {}", fileName, messageId, e);
            auditService.log(account.getId(), "ERROR", messageId, fileName, e.getMessage());
            emailMetrics.recordError();
        }
    }

    private Long uploadAndCreateDocument(EmailAccount account, String fileName,
                                         byte[] content, String contentType) {
        try {
            MultipartFile multipartFile = new ByteArrayMultipartFile(
                    "file", fileName, contentType, content);

            DocumentCreateDTO data = DocumentCreateDTO.builder()
                    .title(fileName)
                    .description("Email attachment from " + account.getEmailAddress())
                    .build();

            var response = documentServiceClient.createDocument(
                    account.getUserId(), multipartFile, data);

            if (response != null && response.getData() != null) {
                return response.getData().getId();
            }
            return null;
        } catch (Exception e) {
            throw new RuntimeException("Document creation via Feign failed: " + e.getMessage(), e);
        }
    }

    private static class ByteArrayMultipartFile implements MultipartFile {
        private final String name;
        private final String originalFilename;
        private final String contentType;
        private final byte[] content;

        ByteArrayMultipartFile(String name, String originalFilename, String contentType, byte[] content) {
            this.name = name;
            this.originalFilename = originalFilename;
            this.contentType = contentType;
            this.content = content;
        }

        @Override public String getName() { return name; }
        @Override public String getOriginalFilename() { return originalFilename; }
        @Override public String getContentType() { return contentType; }
        @Override public boolean isEmpty() { return content.length == 0; }
        @Override public long getSize() { return content.length; }
        @Override public byte[] getBytes() { return content; }
        @Override public InputStream getInputStream() { return new ByteArrayInputStream(content); }
        @Override public void transferTo(java.io.File dest) throws java.io.IOException {
            java.nio.file.Files.write(dest.toPath(), content);
        }
    }

    private byte[] readAttachment(BodyPart bodyPart) throws Exception {
        InputStream is = bodyPart.getInputStream();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int len;
        while ((len = is.read(buffer)) != -1) {
            baos.write(buffer, 0, len);
        }
        return baos.toByteArray();
    }

    public Store connectToImapInternal(EmailAccount account) throws MessagingException {
        return connectToImap(account);
    }

    private Store connectToImap(EmailAccount account) throws MessagingException {
        Properties props = new Properties();
        String protocol = Boolean.TRUE.equals(account.getUseSsl()) ? "imaps" : "imap";

        props.put("mail.store.protocol", protocol);
        props.put("mail." + protocol + ".host", account.getImapHost());
        props.put("mail." + protocol + ".port", String.valueOf(account.getImapPort()));
        props.put("mail." + protocol + ".connectiontimeout", String.valueOf(connectionTimeout));
        props.put("mail." + protocol + ".timeout", String.valueOf(readTimeout));

        if (Boolean.TRUE.equals(account.getUseSsl())) {
            props.put("mail.imaps.ssl.enable", "true");
            props.put("mail.imaps.ssl.trust", "*");
        }

        Session session = Session.getInstance(props);
        Store store = session.getStore(protocol);

        String password = decryptPassword(account.getPasswordEncrypted());
        store.connect(account.getImapHost(), account.getImapPort(), account.getUsername(), password);

        return store;
    }

    private String decryptPassword(String encrypted) {
        try {
            byte[] key = aesKey.getBytes(StandardCharsets.UTF_8);
            SecretKeySpec secretKey = new SecretKeySpec(Arrays.copyOf(key, 32), "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            byte[] decoded = Base64.getDecoder().decode(encrypted);
            return new String(cipher.doFinal(decoded), StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("Failed to decrypt password", e);
            return encrypted;
        }
    }

    public String encryptPassword(String plainPassword) {
        try {
            byte[] key = aesKey.getBytes(StandardCharsets.UTF_8);
            SecretKeySpec secretKey = new SecretKeySpec(Arrays.copyOf(key, 32), "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            return Base64.getEncoder().encodeToString(cipher.doFinal(plainPassword.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new RuntimeException("Failed to encrypt password", e);
        }
    }

    private boolean detectEncrypted(String fileName, byte[] content) {
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".zip") && content.length > 8) {
            return (content[6] & 0x01) != 0;
        }
        if (lower.endsWith(".pdf") && content.length > 128) {
            String header = new String(content, 0, Math.min(content.length, 4096), StandardCharsets.ISO_8859_1);
            return header.contains("/Encrypt");
        }
        return false;
    }

    private boolean isAllowedExtension(String fileName) {
        if (allowedExtensions == null || allowedExtensions.isBlank()) return true;
        String ext = fileName.contains(".") ?
                fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase() : "";
        return allowedExtensions.contains(ext);
    }

    private String sha256(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(data);
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 failed", e);
        }
    }
}
