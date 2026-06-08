package com.doccase.email.service;

import com.doccase.email.domain.entity.EmailAuditLog;

public interface AuditService {

    void log(Long accountId, String action, String messageId, String attachmentName, String detail);
}
