package com.doccase.auth.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("dc_oauth_binding")
public class OAuthBinding {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long userId;
    private String provider;
    private String providerUserId;
    private String accessToken;
    private String refreshToken;
    private LocalDateTime tokenExpiresAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
