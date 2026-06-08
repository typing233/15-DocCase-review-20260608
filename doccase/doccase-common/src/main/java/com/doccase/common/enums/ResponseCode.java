package com.doccase.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ResponseCode {

    SUCCESS(200, "操作成功"),
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未认证"),
    FORBIDDEN(403, "无权限"),
    NOT_FOUND(404, "资源不存在"),
    CONFLICT(409, "资源冲突"),
    TOO_MANY_REQUESTS(429, "请求过于频繁"),
    INTERNAL_ERROR(500, "服务器内部错误"),

    // Auth
    TOKEN_EXPIRED(1001, "Token已过期"),
    TOKEN_INVALID(1002, "Token无效"),
    MFA_REQUIRED(1003, "需要多因素认证"),
    MFA_INVALID(1004, "MFA验证码无效"),
    OAUTH_BINDUNG_EXISTS(1005, "该第三方账号已绑定"),
    ACCOUNT_LOCKED(1006, "账号已锁定"),
    PASSWORD_INCORRECT(1007, "密码错误"),

    // Document
    FILE_NOT_FOUND(2001, "文件不存在"),
    FILE_UPLOAD_FAILED(2002, "文件上传失败"),
    FILE_ALREADY_EXISTS(2003, "文件已存在(秒传)"),
    CHUNK_UPLOAD_EXPIRED(2004, "分片上传已过期"),
    CHUNK_MERGE_FAILED(2005, "分片合并失败"),
    EXPORT_TASK_FAILED(2006, "导出任务失败"),

    // Tag
    TAG_NOT_FOUND(3001, "标签不存在"),
    TAG_NAME_DUPLICATE(3002, "标签名称重复"),
    TAG_MERGE_CONFLICT(3003, "标签合并冲突"),
    TAG_CIRCULAR_REF(3004, "标签存在循环引用"),

    // OCR
    OCR_TASK_NOT_FOUND(4001, "OCR任务不存在"),
    OCR_ENGINE_UNAVAILABLE(4002, "OCR引擎不可用"),
    OCR_PREPROCESSING_FAILED(4003, "图像预处理失败"),
    OCR_RECOGNITION_FAILED(4004, "OCR识别失败"),
    OCR_CONFIDENCE_TOO_LOW(4005, "OCR识别置信度过低"),

    // Search
    SEARCH_INDEX_FAILED(5001, "索引更新失败"),
    SEARCH_QUERY_FAILED(5002, "搜索查询失败");

    private final int code;
    private final String message;
}
