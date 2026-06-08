package com.doccase.document.service;

import com.doccase.document.domain.vo.ChunkUploadRequest;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

public interface UploadService {

    Map<String, Object> initChunkUpload(Long userId, ChunkUploadRequest request);

    Map<String, Object> uploadChunk(String uploadId, Integer chunkIndex, MultipartFile chunk);

    Map<String, Object> mergeChunks(String uploadId, Long userId);

    Map<String, Object> checkInstantUpload(String fileHash, Long userId);

    List<Integer> getUploadedChunks(String uploadId);
}
