package com.doccase.common.constant;

public final class RedisKeyConstants {

    private RedisKeyConstants() {}

    public static final String TOKEN_PREFIX = "auth:token:";
    public static final String REFRESH_TOKEN_PREFIX = "auth:refresh:";
    public static final String USER_CACHE_PREFIX = "user:info:";
    public static final String MFA_CODE_PREFIX = "auth:mfa:";

    public static final String UPLOAD_SESSION_PREFIX = "upload:session:";
    public static final String UPLOAD_CHUNK_PREFIX = "upload:chunk:";
    public static final String FILE_HASH_PREFIX = "file:hash:";

    public static final String TAG_TREE_CACHE = "tag:tree";
    public static final String TAG_LOCK_PREFIX = "tag:lock:";

    public static final String OCR_TASK_PREFIX = "ocr:task:";
    public static final String OCR_LOCK_PREFIX = "ocr:lock:";

    public static final String RATE_LIMIT_PREFIX = "rate:limit:";
    public static final String DISTRIBUTED_LOCK_PREFIX = "lock:";
}
