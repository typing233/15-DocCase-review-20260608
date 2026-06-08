package com.doccase.user.domain.vo;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserUpdateRequest {

    @Size(min = 3, max = 64, message = "用户名长度3-64字符")
    private String username;

    @Email(message = "邮箱格式不正确")
    private String email;

    private String phone;
    private String avatarUrl;
    private Integer status;
}
