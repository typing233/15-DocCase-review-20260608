package com.doccase.email.service.impl;

import com.doccase.email.domain.entity.EmailAuditLog;
import com.doccase.email.mapper.EmailAuditLogMapper;
import com.doccase.email.service.AuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditServiceImpl implements AuditService {

    private final EmailAuditLogMapper auditLogMapper;

    @Override
    @Async
    public void log(Long accountId, String action, String messageId, String attachmentName, String detail) {
        EmailAuditLog auditLog = new EmailAuditLog();
        auditLog.setAccountId(accountId);
        auditLog.setAction(action);
        auditLog.setMessageId(messageId);
        auditLog.setAttachmentName(attachmentName);
        auditLog.setDetail(detail);
        auditLog.setCreatedAt(LocalDateTime.now());
        auditLogMapper.insert(auditLog);

        log.debug("Audit: account={}, action={}, messageId={}, attachment={}", accountId, action, messageId, attachmentName);
    }
}
