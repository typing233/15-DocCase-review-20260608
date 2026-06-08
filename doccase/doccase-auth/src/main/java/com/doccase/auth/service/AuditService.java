package com.doccase.auth.service;

public interface AuditService {

    void log(Long userId, String action, String resourceType, Long resourceId, String detail, String ipAddress, String userAgent);

    void logLogin(Long userId, String ipAddress, String userAgent, boolean success);

    void logLogout(Long userId, String ipAddress);
}
