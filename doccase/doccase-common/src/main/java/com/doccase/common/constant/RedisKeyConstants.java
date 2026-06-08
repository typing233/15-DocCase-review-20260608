package com.doccase.common.constant;

public final class RedisKeyConstants {

    private RedisKeyConstants() {}

    // Auth
    public static final String TOKEN_PREFIX = "auth:token:";
    public static final String REFRESH_TOKEN_PREFIX = "auth:refresh:";
    public static final String USER_CACHE_PREFIX = "user:info:";
    public static final String MFA_CODE_PREFIX = "auth:mfa:";

    // Upload
    public static final String UPLOAD_SESSION_PREFIX = "upload:session:";
    public static final String UPLOAD_CHUNK_PREFIX = "upload:chunk:";
    public static final String FILE_HASH_PREFIX = "file:hash:";

    // Tag
    public static final String TAG_TREE_CACHE = "tag:tree";
    public static final String TAG_TREE_TENANT_PREFIX = "tag:tree:tenant:";
    public static final String TAG_TREE_VERSION_PREFIX = "tag:tree:version:";
    public static final String TAG_LOCK_PREFIX = "tag:lock:";
    public static final String TAG_BLOOM_PREFIX = "tag:bloom:";
    public static final String TAG_BATCH_LOCK_PREFIX = "tag:batch:lock:";

    // OCR
    public static final String OCR_TASK_PREFIX = "ocr:task:";
    public static final String OCR_LOCK_PREFIX = "ocr:lock:";

    // Search
    public static final String SEARCH_EMBEDDING_CACHE_PREFIX = "search:embedding:";
    public static final String SEARCH_REINDEX_PROGRESS = "search:reindex:progress";
    public static final String SEARCH_REINDEX_LOCK = "search:reindex:lock";
    public static final String SEARCH_BULK_BUFFER = "search:bulk:buffer";

    // Rule Engine
    public static final String RULE_CACHE_TENANT_PREFIX = "rule:cache:tenant:";
    public static final String RULE_RELOAD_CHANNEL = "doccase:rule:reload";
    public static final String RULE_METRICS_PREFIX = "rule:metrics:";

    // Email
    public static final String EMAIL_POLL_LOCK_PREFIX = "email:poll:lock:";
    public static final String EMAIL_DEDUP_PREFIX = "email:dedup:";
    public static final String EMAIL_ACCOUNT_CACHE_PREFIX = "email:account:";

    // General
    public static final String RATE_LIMIT_PREFIX = "rate:limit:";
    public static final String DISTRIBUTED_LOCK_PREFIX = "lock:";
}
