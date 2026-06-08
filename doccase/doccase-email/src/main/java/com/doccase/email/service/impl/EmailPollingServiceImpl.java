package com.doccase.email.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.doccase.common.constant.MqConstants;
import com.doccase.common.constant.RedisKeyConstants;
import com.doccase.common.dto.EmailArchiveEvent;
import com.doccase.common.util.DistributedLockUtil;
import com.doccase.email.domain.entity.EmailAccount;
import com.doccase.email.domain.entity.EmailArchiveRecord;
import com.doccase.email.mapper.EmailAccountMapper;
import com.doccase.email.mapper.EmailArchiveRecordMapper;
import com.doccase.email.metrics.EmailMetrics;
import com.doccase.email.service.AuditService;
import com.doccase.email.service.EmailPollingService;
import jakarta.mail.*;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.search.ComparisonTerm;
import jakarta.mail.search.SearchTerm;
import jakarta.mail.search.SentDateTerm;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
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

    private void doPoll(EmailAccount account) {
        auditService.log(account.getId(), "POLL_START", null, null,
                "Starting poll for " + account.getEmailAddress());

        Store store = null;
        try {
            store = connectToImap(account);
            String[] folders = account.getFolderFilter() != null ?
                    account.getFolderFilter().split(",") : new String[]{"INBOX"};

            int totalProcessed = 0;

            for (String folderName : folders) {
                Folder folder = store.getFolder(folderName.trim());
                if (!folder.exists()) {
                    log.warn("Folder {} does not exist for account {}", folderName, account.getEmailAddress());
                    continue;
                }
                folder.open(Folder.READ_ONLY);

                Message[] messages = folder.getMessages();
                int startIdx = Math.max(0, messages.length - maxMessagesPerPoll);

                for (int i = startIdx; i < messages.length && totalProcessed < maxMessagesPerPoll; i++) {
                    Message message = messages[i];
                    if (message instanceof MimeMessage mimeMessage) {
                        processMessage(account, mimeMessage);
                        totalProcessed++;
                    }
                }

                folder.close(false);
            }

            account.setLastPollAt(LocalDateTime.now());
            account.setStatus(1);
            account.setErrorMessage(null);
            accountMapper.updateById(account);

            emailMetrics.recordPoll(account.getId(), totalProcessed);
            auditService.log(account.getId(), "POLL_END", null, null,
                    "Processed " + totalProcessed + " messages");

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

    private void processMessage(EmailAccount account, MimeMessage message) throws Exception {
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

                    // Check extension filter
                    if (!isAllowedExtension(fileName)) {
                        auditService.log(account.getId(), "SKIP", messageId, fileName, "extension not allowed");
                        continue;
                    }

                    // Check size
                    int size = bodyPart.getSize();
                    if (size > maxAttachmentSizeMb * 1024 * 1024) {
                        auditService.log(account.getId(), "SKIP", messageId, fileName, "exceeds max size");
                        continue;
                    }

                    processAttachment(account, messageId, from, subject, receivedDate, bodyPart, fileName);
                }
            }
        }
    }

    private void processAttachment(EmailAccount account, String messageId, String from,
                                   String subject, Date receivedDate, BodyPart bodyPart, String fileName) {
        try {
            InputStream is = bodyPart.getInputStream();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int len;
            while ((len = is.read(buffer)) != -1) {
                baos.write(buffer, 0, len);
            }
            byte[] attachmentBytes = baos.toByteArray();

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

            // Detect encrypted content
            boolean isEncrypted = detectEncrypted(fileName, attachmentBytes);

            // Create archive record
            EmailArchiveRecord record = new EmailArchiveRecord();
            record.setAccountId(account.getId());
            record.setTenantId(account.getTenantId());
            record.setMessageId(messageId);
            record.setMessageUid(0L);
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
            record.setStatus(isEncrypted ? 3 : 1); // 3=skipped if encrypted, 1=archived
            record.setRetryCount(0);
            record.setCreatedAt(LocalDateTime.now());
            record.setUpdatedAt(LocalDateTime.now());

            if (isEncrypted) {
                record.setSkipReason("encrypted attachment");
                archiveRecordMapper.insert(record);
                auditService.log(account.getId(), "SKIP", messageId, fileName, "encrypted");
                emailMetrics.recordEncrypted();
                return;
            }

            archiveRecordMapper.insert(record);

            // Publish archive event for document creation
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
                    "size=" + attachmentBytes.length + ", hash=" + hash);
            emailMetrics.recordArchived();

        } catch (Exception e) {
            log.error("Failed to process attachment {} from message {}", fileName, messageId, e);
            auditService.log(account.getId(), "ERROR", messageId, fileName, e.getMessage());
            emailMetrics.recordError();
        }
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
        // Encrypted ZIP detection (PKZip with encryption flag)
        if (lower.endsWith(".zip") && content.length > 8) {
            return (content[6] & 0x01) != 0;
        }
        // Password-protected PDF
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
