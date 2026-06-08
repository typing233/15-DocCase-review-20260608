package com.doccase.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum StorageType {

    MINIO("minio", "MinIO对象存储"),
    LOCAL("local", "本地文件存储");

    private final String code;
    private final String desc;
}
