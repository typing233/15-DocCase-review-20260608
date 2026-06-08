package com.doccase.document.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.doccase.common.constant.RedisKeyConstants;
import com.doccase.common.enums.ResponseCode;
import com.doccase.common.exception.BizException;
import com.doccase.common.util.FileUtil;
import com.doccase.document.domain.entity.ChunkUploadRecord;
import com.doccase.document.domain.entity.Document;
import com.doccase.document.domain.vo.ChunkUploadRequest;
import com.doccase.document.mapper.DocumentMapper;
import com.doccase.document.service.UploadService;
import com.doccase.document.storage.StorageFactory;
import com.doccase.document.storage.StorageStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class UploadServiceImpl implements UploadService {

    private final StringRedisTemplate redisTemplate;
    private final DocumentMapper documentMapper;
    private final StorageFactory storageFactory;

    @Value("${doccase.storage.type:minio}")
    private String storageType;

    @Value("${doccase.upload.chunk-expire-hours:24}")
    private int chunkExpireHours;

    @Override
    public Map<String, Object> initChunkUpload(Long userId, ChunkUploadRequest request) {
        // Check for instant upload
        if (request.getFileHash() != null) {
            Document existing = documentMapper.selectOne(
                    new LambdaQueryWrapper<Document>()
                            .eq(Document::getFileHash, request.getFileHash())
                            .eq(Document::getIsDeleted, 0)
                            .last("LIMIT 1"));
            if (existing != null) {
                return Map.of("instantUpload", true, "documentId", existing.getId());
            }
        }

        String uploadId = UUID.randomUUID().toString().replace("-", "");
        int totalChunks = (int) Math.ceil((double) request.getTotalSize() / request.getChunkSize());

        String key = RedisKeyConstants.UPLOAD_SESSION_PREFIX + uploadId;
        Map<String, String> sessionData = new HashMap<>();
        sessionData.put("userId", userId.toString());
        sessionData.put("fileName", request.getFileName());
        sessionData.put("fileHash", request.getFileHash() != null ? request.getFileHash() : "");
        sessionData.put("totalSize", request.getTotalSize().toString());
        sessionData.put("chunkSize", request.getChunkSize().toString());
        sessionData.put("totalChunks", String.valueOf(totalChunks));
        sessionData.put("status", "uploading");

        redisTemplate.opsForHash().putAll(key, sessionData);
        redisTemplate.expire(key, chunkExpireHours, TimeUnit.HOURS);

        return Map.of(
                "uploadId", uploadId,
                "totalChunks", totalChunks,
                "chunkSize", request.getChunkSize(),
                "instantUpload", false
        );
    }

    @Override
    public Map<String, Object> uploadChunk(String uploadId, Integer chunkIndex, MultipartFile chunk) {
        String sessionKey = RedisKeyConstants.UPLOAD_SESSION_PREFIX + uploadId;
        if (!Boolean.TRUE.equals(redisTemplate.hasKey(sessionKey))) {
            throw new BizException(ResponseCode.CHUNK_UPLOAD_EXPIRED);
        }

        String chunkKey = RedisKeyConstants.UPLOAD_CHUNK_PREFIX + uploadId;
        StorageStrategy strategy = storageFactory.getStrategy(storageType);

        try {
            String chunkObjectKey = "chunks/" + uploadId + "/" + chunkIndex;
            strategy.upload(chunk.getInputStream(), chunkObjectKey, "application/octet-stream", chunk.getSize());

            redisTemplate.opsForSet().add(chunkKey, chunkIndex.toString());

            Long uploaded = redisTemplate.opsForSet().size(chunkKey);
            String totalStr = (String) redisTemplate.opsForHash().get(sessionKey, "totalChunks");
            int total = Integer.parseInt(totalStr != null ? totalStr : "0");

            return Map.of(
                    "chunkIndex", chunkIndex,
                    "uploaded", uploaded != null ? uploaded : 0,
                    "total", total,
                    "completed", uploaded != null && uploaded == total
            );
        } catch (IOException e) {
            throw new BizException(ResponseCode.FILE_UPLOAD_FAILED, e.getMessage());
        }
    }

    @Override
    public Map<String, Object> mergeChunks(String uploadId, Long userId) {
        String sessionKey = RedisKeyConstants.UPLOAD_SESSION_PREFIX + uploadId;
        Map<Object, Object> session = redisTemplate.opsForHash().entries(sessionKey);
        if (session.isEmpty()) {
            throw new BizException(ResponseCode.CHUNK_UPLOAD_EXPIRED);
        }

        String fileName = (String) session.get("fileName");
        int totalChunks = Integer.parseInt((String) session.get("totalChunks"));
        long totalSize = Long.parseLong((String) session.get("totalSize"));

        StorageStrategy strategy = storageFactory.getStrategy(storageType);
        String ext = FileUtil.getExtension(fileName);
        String finalKey = FileUtil.generateObjectKey("documents/" + userId, fileName);

        try {
            // Merge chunks into a single stream
            ByteArrayOutputStream merged = new ByteArrayOutputStream();
            for (int i = 0; i < totalChunks; i++) {
                String chunkKey = "chunks/" + uploadId + "/" + i;
                try (InputStream chunkStream = strategy.download(chunkKey)) {
                    chunkStream.transferTo(merged);
                }
            }

            byte[] data = merged.toByteArray();
            strategy.upload(new ByteArrayInputStream(data), finalKey, FileUtil.getMimeType(ext), data.length);

            // Clean up chunks
            for (int i = 0; i < totalChunks; i++) {
                strategy.delete("chunks/" + uploadId + "/" + i);
            }

            // Clean up Redis
            redisTemplate.delete(sessionKey);
            redisTemplate.delete(RedisKeyConstants.UPLOAD_CHUNK_PREFIX + uploadId);

            return Map.of(
                    "storagePath", finalKey,
                    "fileSize", (long) data.length,
                    "fileName", fileName
            );
        } catch (IOException e) {
            throw new BizException(ResponseCode.CHUNK_MERGE_FAILED, e.getMessage());
        }
    }

    @Override
    public Map<String, Object> checkInstantUpload(String fileHash, Long userId) {
        Document existing = documentMapper.selectOne(
                new LambdaQueryWrapper<Document>()
                        .eq(Document::getFileHash, fileHash)
                        .eq(Document::getIsDeleted, 0)
                        .last("LIMIT 1"));
        if (existing != null) {
            return Map.of("exists", true, "documentId", existing.getId());
        }
        return Map.of("exists", false);
    }

    @Override
    public List<Integer> getUploadedChunks(String uploadId) {
        String chunkKey = RedisKeyConstants.UPLOAD_CHUNK_PREFIX + uploadId;
        Set<String> members = redisTemplate.opsForSet().members(chunkKey);
        if (members == null) return List.of();
        return members.stream().map(Integer::parseInt).sorted().toList();
    }
}
