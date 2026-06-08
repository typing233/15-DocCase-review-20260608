package com.doccase.auth.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.doccase.auth.domain.entity.OAuthBinding;
import com.doccase.auth.domain.vo.TokenResponse;
import com.doccase.auth.feign.UserFeignClient;
import com.doccase.auth.mapper.OAuthBindingMapper;
import com.doccase.auth.service.TokenService;
import com.doccase.common.dto.UserDTO;
import com.doccase.common.enums.ResponseCode;
import com.doccase.common.exception.AuthException;
import com.doccase.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/auth/oauth")
@RequiredArgsConstructor
public class OAuthController {

    private final OAuthBindingMapper oAuthBindingMapper;
    private final UserFeignClient userFeignClient;
    private final TokenService tokenService;

    @PostMapping("/bind")
    public ApiResponse<Void> bindAccount(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody Map<String, String> body) {
        String provider = body.get("provider");
        String providerUserId = body.get("providerUserId");
        String accessToken = body.get("accessToken");
        String refreshToken = body.get("refreshToken");

        LambdaQueryWrapper<OAuthBinding> existQuery = new LambdaQueryWrapper<>();
        existQuery.eq(OAuthBinding::getProvider, provider)
                .eq(OAuthBinding::getProviderUserId, providerUserId);
        if (oAuthBindingMapper.selectCount(existQuery) > 0) {
            throw new AuthException(ResponseCode.OAUTH_BINDUNG_EXISTS);
        }

        OAuthBinding binding = new OAuthBinding();
        binding.setUserId(userId);
        binding.setProvider(provider);
        binding.setProviderUserId(providerUserId);
        binding.setAccessToken(accessToken);
        binding.setRefreshToken(refreshToken);
        binding.setCreatedAt(LocalDateTime.now());
        binding.setUpdatedAt(LocalDateTime.now());
        oAuthBindingMapper.insert(binding);

        return ApiResponse.success();
    }

    @DeleteMapping("/unbind")
    public ApiResponse<Void> unbindAccount(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam String provider) {
        LambdaQueryWrapper<OAuthBinding> query = new LambdaQueryWrapper<>();
        query.eq(OAuthBinding::getUserId, userId)
                .eq(OAuthBinding::getProvider, provider);
        oAuthBindingMapper.delete(query);
        return ApiResponse.success();
    }

    @PostMapping("/callback/{provider}")
    public ApiResponse<TokenResponse> oauthCallback(
            @PathVariable String provider,
            @RequestBody Map<String, String> body) {
        String providerUserId = body.get("providerUserId");

        LambdaQueryWrapper<OAuthBinding> query = new LambdaQueryWrapper<>();
        query.eq(OAuthBinding::getProvider, provider)
                .eq(OAuthBinding::getProviderUserId, providerUserId);
        OAuthBinding binding = oAuthBindingMapper.selectOne(query);

        if (binding == null) {
            throw new AuthException(ResponseCode.NOT_FOUND.getCode(), "该第三方账号未绑定任何用户");
        }

        ApiResponse<UserDTO> userResp = userFeignClient.getUserDTO(binding.getUserId());
        if (userResp == null || userResp.getData() == null) {
            throw new AuthException(ResponseCode.NOT_FOUND.getCode(), "绑定的用户不存在");
        }

        UserDTO user = userResp.getData();
        String roles = String.join(",", user.getRoles());
        String accessToken = tokenService.generateAccessToken(user.getId(), user.getUsername(), roles);
        String refreshTokenStr = tokenService.generateRefreshToken(user.getId());

        return ApiResponse.success(TokenResponse.of(accessToken, refreshTokenStr, 900L));
    }

    @GetMapping("/bindings")
    public ApiResponse<List<OAuthBinding>> getBindings(@RequestHeader("X-User-Id") Long userId) {
        LambdaQueryWrapper<OAuthBinding> query = new LambdaQueryWrapper<>();
        query.eq(OAuthBinding::getUserId, userId);
        List<OAuthBinding> bindings = oAuthBindingMapper.selectList(query);
        bindings.forEach(b -> {
            b.setAccessToken(null);
            b.setRefreshToken(null);
        });
        return ApiResponse.success(bindings);
    }
}
