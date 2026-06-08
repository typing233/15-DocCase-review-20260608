package com.doccase.document.controller;

import com.doccase.common.domain.PageResult;
import com.doccase.common.dto.DocumentDTO;
import com.doccase.common.response.ApiResponse;
import com.doccase.document.domain.vo.DocumentCreateRequest;
import com.doccase.document.domain.vo.DocumentQueryRequest;
import com.doccase.document.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    @PostMapping
    public ApiResponse<DocumentDTO> createDocument(
            @RequestHeader("X-User-Id") Long userId,
            @RequestPart("file") MultipartFile file,
            @RequestPart("data") DocumentCreateRequest request) {
        return ApiResponse.success(documentService.createDocument(userId, request, file));
    }

    @GetMapping("/{id}")
    public ApiResponse<DocumentDTO> getDocument(@PathVariable Long id) {
        return ApiResponse.success(documentService.getDocument(id));
    }

    @PostMapping("/query")
    public ApiResponse<PageResult<DocumentDTO>> queryDocuments(@RequestBody DocumentQueryRequest request) {
        return ApiResponse.success(documentService.queryDocuments(request));
    }

    @PatchMapping("/{id}")
    public ApiResponse<DocumentDTO> updateDocument(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody Map<String, Object> updates) {
        return ApiResponse.success(documentService.updateDocument(id, userId, updates));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteDocument(@PathVariable Long id, @RequestHeader("X-User-Id") Long userId) {
        documentService.deleteDocument(id, userId);
        return ApiResponse.success();
    }

    @PostMapping("/{id}/restore/{version}")
    public ApiResponse<Void> restoreVersion(
            @PathVariable Long id,
            @PathVariable Integer version,
            @RequestHeader("X-User-Id") Long userId) {
        documentService.restoreVersion(id, version, userId);
        return ApiResponse.success();
    }
}
