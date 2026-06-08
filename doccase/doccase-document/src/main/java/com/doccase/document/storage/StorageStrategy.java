package com.doccase.document.storage;

import java.io.InputStream;
import java.time.Duration;

public interface StorageStrategy {

    String upload(InputStream input, String objectKey, String contentType, long size);

    InputStream download(String objectKey);

    void delete(String objectKey);

    String getPresignedUrl(String objectKey, Duration expiry);

    boolean exists(String objectKey);
}
