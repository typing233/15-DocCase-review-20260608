package com.doccase.common.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class UserDTO implements Serializable {

    private Long id;
    private String username;
    private String email;
    private String phone;
    private String avatarUrl;
    private Integer status;
    private List<String> roles;
    private LocalDateTime createdAt;
    private String passwordHash;
}
