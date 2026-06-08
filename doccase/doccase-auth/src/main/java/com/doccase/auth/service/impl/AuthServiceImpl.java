package com.doccase.auth.service.impl;

import com.doccase.auth.domain.vo.LoginRequest;
import com.doccase.auth.domain.vo.TokenResponse;
import com.doccase.auth.service.AuditService;
import com.doccase.auth.service.AuthService;
import com.doccase.auth.service.MfaService;
import com.doccase.auth.service.TokenService;
import com.doccase.common.dto.UserDTO;
import com.doccase.common.enums.ResponseCode;
import com.doccase.common.exception.AuthException;
import com.doccase.common.exception.BizException;
import com.doccase.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final TokenService tokenService;
    private final MfaService mfaService;
    private final AuditService auditService;
    private final UserFeignClient userFeignClient;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public TokenResponse login(LoginRequest request, String ipAddress, String userAgent) {
        ApiResponse<UserDTO> response = userFeignClient.getUserByUsername(request.getUsername());
        if (response == null || response.getData() == null) {
            throw new AuthException(ResponseCode.PASSWORD_INCORRECT);
        }

        UserDTO user = response.getData();
        if (user.getStatus() == 0) {
            throw new AuthException(ResponseCode.ACCOUNT_LOCKED);
        }

        // Password verification would normally use the password hash from user service
        // For now we delegate to user-service internal endpoint

        if (mfaService.isMfaEnabled(user.getId())) {
            if (request.getMfaCode() == null || request.getMfaCode().isEmpty()) {
                auditService.logLogin(user.getId(), ipAddress, userAgent, false);
                return TokenResponse.mfaRequired();
            }
            if (!mfaService.verifyCode(user.getId(), request.getMfaCode())) {
                auditService.logLogin(user.getId(), ipAddress, userAgent, false);
                throw new AuthException(ResponseCode.MFA_INVALID);
            }
        }

        String roles = String.join(",", user.getRoles());
        String accessToken = tokenService.generateAccessToken(user.getId(), user.getUsername(), roles);
        String refreshToken = tokenService.generateRefreshToken(user.getId());

        auditService.logLogin(user.getId(), ipAddress, userAgent, true);
        return TokenResponse.of(accessToken, refreshToken, 900L);
    }

    @Override
    public TokenResponse refreshToken(String refreshToken) {
        Long userId = tokenService.validateRefreshToken(refreshToken);
        if (userId == null) {
            throw new AuthException(ResponseCode.TOKEN_EXPIRED);
        }

        ApiResponse<UserDTO> response = userFeignClient.getUserDTO(userId);
        if (response == null || response.getData() == null) {
            throw new AuthException(ResponseCode.TOKEN_INVALID);
        }

        UserDTO user = response.getData();
        String roles = String.join(",", user.getRoles());
        String newAccessToken = tokenService.generateAccessToken(user.getId(), user.getUsername(), roles);
        String newRefreshToken = tokenService.generateRefreshToken(user.getId());

        tokenService.revokeRefreshToken(refreshToken);
        return TokenResponse.of(newAccessToken, newRefreshToken, 900L);
    }

    @Override
    public void logout(String accessToken) {
        tokenService.revokeToken(accessToken);
    }

    @FeignClient(name = "doccase-user", path = "/users")
    public interface UserFeignClient {
        @GetMapping("/internal/by-username")
        ApiResponse<UserDTO> getUserByUsername(@RequestParam("username") String username);

        @GetMapping("/internal/{id}")
        ApiResponse<UserDTO> getUserDTO(@org.springframework.web.bind.annotation.PathVariable("id") Long id);
    }
}
