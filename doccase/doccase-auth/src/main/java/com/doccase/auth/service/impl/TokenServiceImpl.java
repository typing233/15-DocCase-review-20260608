package com.doccase.auth.service.impl;

import com.doccase.auth.service.TokenService;
import com.doccase.common.constant.RedisKeyConstants;
import com.doccase.common.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class TokenServiceImpl implements TokenService {

    private final StringRedisTemplate redisTemplate;

    @Value("${doccase.jwt.secret}")
    private String jwtSecret;

    @Value("${doccase.jwt.access-token-expiration:900000}")
    private long accessTokenExpiration; // 15 minutes

    @Value("${doccase.jwt.refresh-token-expiration:604800000}")
    private long refreshTokenExpiration; // 7 days

    @Override
    public String generateAccessToken(Long userId, String username, String roles) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", userId.toString());
        claims.put("username", username);
        claims.put("roles", roles);
        String token = JwtUtil.generateToken(jwtSecret, accessTokenExpiration, claims);
        redisTemplate.opsForValue().set(
                RedisKeyConstants.TOKEN_PREFIX + userId,
                token,
                accessTokenExpiration, TimeUnit.MILLISECONDS);
        return token;
    }

    @Override
    public String generateRefreshToken(Long userId) {
        String refreshToken = UUID.randomUUID().toString().replace("-", "");
        redisTemplate.opsForValue().set(
                RedisKeyConstants.REFRESH_TOKEN_PREFIX + refreshToken,
                userId.toString(),
                refreshTokenExpiration, TimeUnit.MILLISECONDS);
        return refreshToken;
    }

    @Override
    public Map<String, Object> validateAccessToken(String token) {
        if (!JwtUtil.validateToken(jwtSecret, token)) {
            return null;
        }
        var claims = JwtUtil.parseToken(jwtSecret, token);
        Map<String, Object> result = new HashMap<>();
        result.put("userId", Long.parseLong(claims.getSubject()));
        result.put("username", claims.get("username", String.class));
        result.put("roles", claims.get("roles", String.class));
        return result;
    }

    @Override
    public Long validateRefreshToken(String refreshToken) {
        String userId = redisTemplate.opsForValue().get(RedisKeyConstants.REFRESH_TOKEN_PREFIX + refreshToken);
        if (userId == null) return null;
        return Long.parseLong(userId);
    }

    @Override
    public void revokeToken(String token) {
        if (token != null && JwtUtil.validateToken(jwtSecret, token)) {
            var claims = JwtUtil.parseToken(jwtSecret, token);
            redisTemplate.delete(RedisKeyConstants.TOKEN_PREFIX + claims.getSubject());
        }
    }

    @Override
    public void revokeRefreshToken(String refreshToken) {
        redisTemplate.delete(RedisKeyConstants.REFRESH_TOKEN_PREFIX + refreshToken);
    }
}
