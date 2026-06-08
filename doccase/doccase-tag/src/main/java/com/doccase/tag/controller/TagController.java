package com.doccase.tag.controller;

import com.doccase.common.response.ApiResponse;
import com.doccase.tag.domain.entity.Tag;
import com.doccase.tag.domain.vo.*;
import com.doccase.tag.service.TagService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/tags")
@RequiredArgsConstructor
public class TagController {

    private final TagService tagService;

    @PostMapping
    public ApiResponse<Tag> createTag(
            @RequestHeader(value = "X-User-Id", defaultValue = "0") Long userId,
            @RequestHeader(value = "X-Tenant-Id", defaultValue = "default") String tenantId,
            @RequestBody @Valid TagCreateRequest request) {
        return ApiResponse.success(tagService.createTag(tenantId, userId, request));
    }

    @PutMapping("/{id}")
    public ApiResponse<Tag> updateTag(
            @PathVariable Long id,
            @RequestHeader(value = "X-Tenant-Id", defaultValue = "default") String tenantId,
            @RequestBody @Valid TagCreateRequest request) {
        return ApiResponse.success(tagService.updateTag(tenantId, id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteTag(
            @PathVariable Long id,
            @RequestHeader(value = "X-Tenant-Id", defaultValue = "default") String tenantId) {
        tagService.deleteTag(tenantId, id);
        return ApiResponse.success();
    }

    @GetMapping("/tree")
    public ApiResponse<List<TagTreeVO>> getTagTree(
            @RequestHeader(value = "X-Tenant-Id", defaultValue = "default") String tenantId) {
        return ApiResponse.success(tagService.getTagTree(tenantId));
    }

    @PostMapping("/merge")
    public ApiResponse<Void> mergeTag(
            @RequestHeader(value = "X-User-Id", defaultValue = "0") Long userId,
            @RequestHeader(value = "X-Tenant-Id", defaultValue = "default") String tenantId,
            @RequestBody @Valid TagMergeRequest request) {
        tagService.mergeTag(tenantId, userId, request);
        return ApiResponse.success();
    }

    @PostMapping("/import")
    public ApiResponse<Void> importTags(
            @RequestHeader(value = "X-User-Id", defaultValue = "0") Long userId,
            @RequestHeader(value = "X-Tenant-Id", defaultValue = "default") String tenantId,
            @RequestBody List<TagCreateRequest> tags) {
        tagService.importTags(tenantId, tags, userId);
        return ApiResponse.success();
    }

    @GetMapping("/export")
    public ApiResponse<List<Tag>> exportTags(
            @RequestHeader(value = "X-Tenant-Id", defaultValue = "default") String tenantId) {
        return ApiResponse.success(tagService.exportTags(tenantId));
    }

    @PostMapping("/documents/{documentId}/tags/{tagId}")
    public ApiResponse<Void> addDocumentTag(
            @PathVariable Long documentId,
            @PathVariable Long tagId,
            @RequestHeader(value = "X-Tenant-Id", defaultValue = "default") String tenantId) {
        tagService.addDocumentTag(tenantId, documentId, tagId);
        return ApiResponse.success();
    }

    @DeleteMapping("/documents/{documentId}/tags/{tagId}")
    public ApiResponse<Void> removeDocumentTag(
            @PathVariable Long documentId,
            @PathVariable Long tagId,
            @RequestHeader(value = "X-Tenant-Id", defaultValue = "default") String tenantId) {
        tagService.removeDocumentTag(tenantId, documentId, tagId);
        return ApiResponse.success();
    }

    @GetMapping("/documents/{documentId}/tags")
    public ApiResponse<List<Tag>> getDocumentTags(
            @PathVariable Long documentId,
            @RequestHeader(value = "X-Tenant-Id", defaultValue = "default") String tenantId) {
        return ApiResponse.success(tagService.getDocumentTags(tenantId, documentId));
    }

    @GetMapping("/documents/{documentId}/tags/inherited")
    public ApiResponse<List<Tag>> getDocumentTagsWithInheritance(
            @PathVariable Long documentId,
            @RequestHeader(value = "X-Tenant-Id", defaultValue = "default") String tenantId) {
        return ApiResponse.success(tagService.getDocumentTagsWithInheritance(tenantId, documentId));
    }

    @GetMapping("/inherited/{tagId}/documents")
    public ApiResponse<List<Long>> getInheritedDocumentIds(
            @PathVariable Long tagId,
            @RequestHeader(value = "X-Tenant-Id", defaultValue = "default") String tenantId) {
        return ApiResponse.success(tagService.getInheritedDocumentIds(tenantId, tagId));
    }

    @PostMapping("/batch/tag")
    public ApiResponse<Void> batchTag(
            @RequestHeader(value = "X-User-Id", defaultValue = "0") Long userId,
            @RequestHeader(value = "X-Tenant-Id", defaultValue = "default") String tenantId,
            @RequestBody @Valid BatchTagRequest request) {
        tagService.batchTag(tenantId, userId, request);
        return ApiResponse.success();
    }

    @PostMapping("/batch/move")
    public ApiResponse<Void> batchMove(
            @RequestHeader(value = "X-User-Id", defaultValue = "0") Long userId,
            @RequestHeader(value = "X-Tenant-Id", defaultValue = "default") String tenantId,
            @RequestBody @Valid BatchMoveRequest request) {
        tagService.batchMove(tenantId, userId, request);
        return ApiResponse.success();
    }

    @PostMapping("/batch/delete")
    public ApiResponse<Void> batchDelete(
            @RequestHeader(value = "X-User-Id", defaultValue = "0") Long userId,
            @RequestHeader(value = "X-Tenant-Id", defaultValue = "default") String tenantId,
            @RequestBody @Valid BatchDeleteRequest request) {
        tagService.batchDelete(tenantId, userId, request);
        return ApiResponse.success();
    }
}
