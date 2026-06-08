package com.doccase.document.storage;

import io.minio.*;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component("minioStorage")
@RequiredArgsConstructor
public class MinioStorageStrategy implements StorageStrategy {

    private final MinioClient minioClient;

    @Value("${doccase.storage.minio.bucket:doccase}")
    private String bucketName;

    @Override
    public String upload(InputStream input, String objectKey, String contentType, long size) {
        try {
            ensureBucketExists();
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectKey)
                    .stream(input, size, -1)
                    .contentType(contentType)
                    .build());
            return objectKey;
        } catch (Exception e) {
            throw new RuntimeException("MinIO upload failed: " + e.getMessage(), e);
        }
    }

    @Override
    public InputStream download(String objectKey) {
        try {
            return minioClient.getObject(GetObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectKey)
                    .build());
        } catch (Exception e) {
            throw new RuntimeException("MinIO download failed: " + e.getMessage(), e);
        }
    }

    @Override
    public void delete(String objectKey) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectKey)
                    .build());
        } catch (Exception e) {
            log.warn("MinIO delete failed for {}: {}", objectKey, e.getMessage());
        }
    }

    @Override
    public String getPresignedUrl(String objectKey, Duration expiry) {
        try {
            return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .bucket(bucketName)
                    .object(objectKey)
                    .method(Method.GET)
                    .expiry((int) expiry.toSeconds(), TimeUnit.SECONDS)
                    .build());
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate presigned URL: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean exists(String objectKey) {
        try {
            minioClient.statObject(StatObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectKey)
                    .build());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void ensureBucketExists() {
        try {
            if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build())) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
            }
        } catch (Exception e) {
            log.error("Failed to ensure bucket exists: {}", e.getMessage());
        }
    }
}
