package com.doccase.auth.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("dc_mfa_secret")
public class MfaSecret {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long userId;
    private String secretKey;
    private Integer isEnabled;
    private String backupCodes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
