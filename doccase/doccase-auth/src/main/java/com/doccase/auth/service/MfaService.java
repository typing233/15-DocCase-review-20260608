package com.doccase.auth.service;

public interface MfaService {

    String generateSecret(Long userId);

    String getQrCodeUri(Long userId, String username);

    boolean verifyCode(Long userId, String code);

    void enableMfa(Long userId);

    void disableMfa(Long userId);

    boolean isMfaEnabled(Long userId);
}
