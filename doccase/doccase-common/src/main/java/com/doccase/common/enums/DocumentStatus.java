package com.doccase.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum DocumentStatus {

    PROCESSING(0, "处理中"),
    ACTIVE(1, "正常"),
    ARCHIVED(2, "已归档");

    private final int code;
    private final String desc;
}
