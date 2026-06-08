package com.doccase.auth.service;

import java.util.Map;

public interface TokenService {

    String generateAccessToken(Long userId, String username, String roles);

    String generateRefreshToken(Long userId);

    Map<String, Object> validateAccessToken(String token);

    Long validateRefreshToken(String refreshToken);

    void revokeToken(String token);

    void revokeRefreshToken(String refreshToken);
}
