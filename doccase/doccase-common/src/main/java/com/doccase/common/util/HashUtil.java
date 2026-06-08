package com.doccase.common.util;

import cn.hutool.crypto.digest.DigestUtil;

import java.io.InputStream;

public final class HashUtil {

    private HashUtil() {}

    public static String sha256(byte[] data) {
        return DigestUtil.sha256Hex(data);
    }

    public static String sha256(InputStream is) {
        return DigestUtil.sha256Hex(is);
    }

    public static String md5(byte[] data) {
        return DigestUtil.md5Hex(data);
    }

    public static String md5(InputStream is) {
        return DigestUtil.md5Hex(is);
    }
}
