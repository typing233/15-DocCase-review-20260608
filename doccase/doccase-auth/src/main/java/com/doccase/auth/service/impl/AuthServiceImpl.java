package com.doccase.auth.service.impl;

import com.doccase.auth.domain.vo.LoginRequest;
import com.doccase.auth.domain.vo.RegisterRequest;
import com.doccase.auth.domain.vo.TokenResponse;
import com.doccase.auth.feign.UserFeignClient;
import com.doccase.auth.service.AuditService;
import com.doccase.auth.service.AuthService;
import com.doccase.auth.service.MfaService;
import com.doccase.auth.service.TokenService;
import com.doccase.common.dto.UserDTO;
import com.doccase.common.enums.ResponseCode;
import com.doccase.common.exception.AuthException;
import com.doccase.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final TokenService tokenService;
    private final MfaService mfaService;
    private final AuditService auditService;
    private final UserFeignClient userFeignClient;
    private final BCryptPasswordEncoder passwordEncoder;

    @Override
    public TokenResponse login(LoginRequest request, String ipAddress, String userAgent) {
        ApiResponse<UserDTO> response = userFeignClient.getUserByUsername(request.getUsername());
        if (response == null || response.getData() == null) {
            throw new AuthException(ResponseCode.PASSWORD_INCORRECT);
        }

        UserDTO user = response.getData();

        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new AuthException(ResponseCode.ACCOUNT_LOCKED);
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            auditService.logLogin(user.getId(), ipAddress, userAgent, false);
            throw new AuthException(ResponseCode.PASSWORD_INCORRECT);
        }

        if (mfaService.isMfaEnabled(user.getId())) {
            if (request.getMfaCode() == null || request.getMfaCode().isEmpty()) {
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
    public TokenResponse register(RegisterRequest request, String ipAddress, String userAgent) {
        Map<String, Object> createReq = new HashMap<>();
        createReq.put("username", request.getUsername());
        createReq.put("password", request.getPassword());
        createReq.put("email", request.getEmail());
        createReq.put("phone", request.getPhone());

        ApiResponse<?> createRes = userFeignClient.createUser(createReq);
        if (createRes == null || createRes.getCode() != 200) {
            String msg = createRes != null ? createRes.getMessage() : "用户创建失败";
            throw new AuthException(ResponseCode.REGISTER_FAILED.getCode(), msg);
        }

        LoginRequest loginReq = new LoginRequest();
        loginReq.setUsername(request.getUsername());
        loginReq.setPassword(request.getPassword());
        return login(loginReq, ipAddress, userAgent);
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
}
