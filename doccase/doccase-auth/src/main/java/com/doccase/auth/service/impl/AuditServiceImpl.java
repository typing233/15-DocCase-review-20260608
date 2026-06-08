package com.doccase.auth.service.impl;

import com.doccase.auth.domain.entity.AuditLog;
import com.doccase.auth.mapper.AuditLogMapper;
import com.doccase.auth.service.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuditServiceImpl implements AuditService {

    private final AuditLogMapper auditLogMapper;

    @Async
    @Override
    public void log(Long userId, String action, String resourceType, Long resourceId,
                    String detail, String ipAddress, String userAgent) {
        AuditLog log = new AuditLog();
        log.setUserId(userId);
        log.setAction(action);
        log.setResourceType(resourceType);
        log.setResourceId(resourceId);
        log.setDetail(detail);
        log.setIpAddress(ipAddress);
        log.setUserAgent(userAgent);
        log.setCreatedAt(LocalDateTime.now());
        auditLogMapper.insert(log);
    }

    @Async
    @Override
    public void logLogin(Long userId, String ipAddress, String userAgent, boolean success) {
        log(userId, success ? "LOGIN_SUCCESS" : "LOGIN_FAILED", "user", userId,
                null, ipAddress, userAgent);
    }

    @Async
    @Override
    public void logLogout(Long userId, String ipAddress) {
        log(userId, "LOGOUT", "user", userId, null, ipAddress, null);
    }
}
