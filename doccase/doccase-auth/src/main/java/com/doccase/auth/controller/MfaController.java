package com.doccase.auth.controller;

import com.doccase.auth.service.MfaService;
import com.doccase.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth/mfa")
@RequiredArgsConstructor
public class MfaController {

    private final MfaService mfaService;

    @PostMapping("/setup")
    public ApiResponse<Map<String, String>> setupMfa(@RequestHeader("X-User-Id") Long userId,
                                                      @RequestHeader("X-Username") String username) {
        String secret = mfaService.generateSecret(userId);
        String qrUri = mfaService.getQrCodeUri(userId, username);
        return ApiResponse.success(Map.of("secret", secret, "qrUri", qrUri != null ? qrUri : ""));
    }

    @PostMapping("/enable")
    public ApiResponse<Void> enableMfa(@RequestHeader("X-User-Id") Long userId,
                                       @RequestBody Map<String, String> body) {
        String code = body.get("code");
        if (!mfaService.verifyCode(userId, code)) {
            return ApiResponse.error(400, "验证码无效，无法启用MFA");
        }
        mfaService.enableMfa(userId);
        return ApiResponse.success();
    }

    @PostMapping("/disable")
    public ApiResponse<Void> disableMfa(@RequestHeader("X-User-Id") Long userId,
                                        @RequestBody Map<String, String> body) {
        String code = body.get("code");
        if (!mfaService.verifyCode(userId, code)) {
            return ApiResponse.error(400, "验证码无效，无法禁用MFA");
        }
        mfaService.disableMfa(userId);
        return ApiResponse.success();
    }

    @GetMapping("/status")
    public ApiResponse<Map<String, Boolean>> getMfaStatus(@RequestHeader("X-User-Id") Long userId) {
        return ApiResponse.success(Map.of("enabled", mfaService.isMfaEnabled(userId)));
    }
}
