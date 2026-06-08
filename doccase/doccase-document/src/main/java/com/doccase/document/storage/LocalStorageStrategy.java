package com.doccase.document.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;

@Slf4j
@Component("localStorage")
public class LocalStorageStrategy implements StorageStrategy {

    @Value("${doccase.storage.local.base-path:/data/doccase/files}")
    private String basePath;

    @Override
    public String upload(InputStream input, String objectKey, String contentType, long size) {
        try {
            Path filePath = Paths.get(basePath, objectKey);
            Files.createDirectories(filePath.getParent());
            Files.copy(input, filePath, StandardCopyOption.REPLACE_EXISTING);
            return objectKey;
        } catch (IOException e) {
            throw new RuntimeException("Local storage upload failed: " + e.getMessage(), e);
        }
    }

    @Override
    public InputStream download(String objectKey) {
        try {
            Path filePath = Paths.get(basePath, objectKey);
            return new FileInputStream(filePath.toFile());
        } catch (FileNotFoundException e) {
            throw new RuntimeException("File not found: " + objectKey, e);
        }
    }

    @Override
    public void delete(String objectKey) {
        try {
            Path filePath = Paths.get(basePath, objectKey);
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            log.warn("Local storage delete failed for {}: {}", objectKey, e.getMessage());
        }
    }

    @Override
    public String getPresignedUrl(String objectKey, Duration expiry) {
        return "/api/documents/download/" + objectKey;
    }

    @Override
    public boolean exists(String objectKey) {
        return Files.exists(Paths.get(basePath, objectKey));
    }
}
