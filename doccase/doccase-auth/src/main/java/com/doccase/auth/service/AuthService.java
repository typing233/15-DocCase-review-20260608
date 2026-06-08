package com.doccase.auth.service;

import com.doccase.auth.domain.vo.LoginRequest;
import com.doccase.auth.domain.vo.TokenResponse;

public interface AuthService {

    TokenResponse login(LoginRequest request, String ipAddress, String userAgent);

    TokenResponse refreshToken(String refreshToken);

    void logout(String accessToken);
}
