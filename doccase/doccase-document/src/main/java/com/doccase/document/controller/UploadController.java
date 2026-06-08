package com.doccase.document.controller;

import com.doccase.common.response.ApiResponse;
import com.doccase.document.domain.vo.ChunkUploadRequest;
import com.doccase.document.service.UploadService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/upload")
@RequiredArgsConstructor
public class UploadController {

    private final UploadService uploadService;

    @PostMapping("/init")
    public ApiResponse<Map<String, Object>> initUpload(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody ChunkUploadRequest request) {
        return ApiResponse.success(uploadService.initChunkUpload(userId, request));
    }

    @PostMapping("/chunk")
    public ApiResponse<Map<String, Object>> uploadChunk(
            @RequestParam String uploadId,
            @RequestParam Integer chunkIndex,
            @RequestPart("chunk") MultipartFile chunk) {
        return ApiResponse.success(uploadService.uploadChunk(uploadId, chunkIndex, chunk));
    }

    @PostMapping("/merge")
    public ApiResponse<Map<String, Object>> mergeChunks(
            @RequestParam String uploadId,
            @RequestHeader("X-User-Id") Long userId) {
        return ApiResponse.success(uploadService.mergeChunks(uploadId, userId));
    }

    @GetMapping("/check")
    public ApiResponse<Map<String, Object>> checkInstantUpload(
            @RequestParam String fileHash,
            @RequestHeader("X-User-Id") Long userId) {
        return ApiResponse.success(uploadService.checkInstantUpload(fileHash, userId));
    }

    @GetMapping("/chunks")
    public ApiResponse<List<Integer>> getUploadedChunks(@RequestParam String uploadId) {
        return ApiResponse.success(uploadService.getUploadedChunks(uploadId));
    }
}
