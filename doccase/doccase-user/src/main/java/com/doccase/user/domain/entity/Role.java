package com.doccase.user.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.doccase.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("dc_role")
public class Role extends BaseEntity {

    private String name;
    private String code;
    private String description;
}
