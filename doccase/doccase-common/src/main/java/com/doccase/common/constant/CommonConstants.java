package com.doccase.common.constant;

public final class CommonConstants {

    private CommonConstants() {}

    public static final String HEADER_USER_ID = "X-User-Id";
    public static final String HEADER_USERNAME = "X-Username";
    public static final String HEADER_ROLES = "X-User-Roles";
    public static final String HEADER_TOKEN = "Authorization";
    public static final String TOKEN_PREFIX = "Bearer ";

    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;

    public static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
}
