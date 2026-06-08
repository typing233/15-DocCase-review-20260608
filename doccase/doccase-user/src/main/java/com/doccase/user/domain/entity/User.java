package com.doccase.user.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.doccase.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("dc_user")
public class User extends BaseEntity {

    private String username;
    private String email;
    private String passwordHash;
    private String avatarUrl;
    private String phone;
    private Integer status;
}
