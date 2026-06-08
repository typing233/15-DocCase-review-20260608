package com.doccase.email.domain.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class EmailAccountCreateRequest {

    @NotBlank(message = "邮箱地址不能为空")
    private String emailAddress;

    @NotBlank(message = "IMAP主机不能为空")
    private String imapHost;

    @NotNull(message = "IMAP端口不能为空")
    private Integer imapPort;

    private Boolean useSsl;

    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;

    private String folderFilter;

    private String attachmentFilter;

    private Integer pollIntervalMinutes;
}
