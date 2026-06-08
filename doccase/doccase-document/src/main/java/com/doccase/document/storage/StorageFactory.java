package com.doccase.document.storage;

import com.doccase.common.enums.StorageType;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class StorageFactory {

    private final Map<String, StorageStrategy> strategies;

    public StorageFactory(Map<String, StorageStrategy> strategies) {
        this.strategies = strategies;
    }

    public StorageStrategy getStrategy(StorageType type) {
        return switch (type) {
            case MINIO -> strategies.get("minioStorage");
            case LOCAL -> strategies.get("localStorage");
        };
    }

    public StorageStrategy getStrategy(String type) {
        return getStrategy(StorageType.valueOf(type.toUpperCase()));
    }
}
