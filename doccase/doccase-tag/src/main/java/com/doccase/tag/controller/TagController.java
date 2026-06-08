package com.doccase.tag.controller;

import com.doccase.common.response.ApiResponse;
import com.doccase.tag.domain.entity.Tag;
import com.doccase.tag.domain.vo.TagCreateRequest;
import com.doccase.tag.domain.vo.TagMergeRequest;
import com.doccase.tag.domain.vo.TagTreeVO;
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
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody TagCreateRequest request) {
        return ApiResponse.success(tagService.createTag(userId, request));
    }

    @PutMapping("/{id}")
    public ApiResponse<Tag> updateTag(
            @PathVariable Long id,
            @Valid @RequestBody TagCreateRequest request) {
        return ApiResponse.success(tagService.updateTag(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteTag(@PathVariable Long id) {
        tagService.deleteTag(id);
        return ApiResponse.success();
    }

    @GetMapping("/tree")
    public ApiResponse<List<TagTreeVO>> getTagTree() {
        return ApiResponse.success(tagService.getTagTree());
    }

    @PostMapping("/merge")
    public ApiResponse<Void> mergeTag(@Valid @RequestBody TagMergeRequest request) {
        tagService.mergeTag(request);
        return ApiResponse.success();
    }

    @PostMapping("/import")
    public ApiResponse<Void> importTags(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody List<TagCreateRequest> tags) {
        tagService.importTags(tags, userId);
        return ApiResponse.success();
    }

    @GetMapping("/export")
    public ApiResponse<List<Tag>> exportTags() {
        return ApiResponse.success(tagService.exportTags());
    }

    @PostMapping("/documents/{documentId}/tags/{tagId}")
    public ApiResponse<Void> addDocumentTag(
            @PathVariable Long documentId,
            @PathVariable Long tagId) {
        tagService.addDocumentTag(documentId, tagId);
        return ApiResponse.success();
    }

    @DeleteMapping("/documents/{documentId}/tags/{tagId}")
    public ApiResponse<Void> removeDocumentTag(
            @PathVariable Long documentId,
            @PathVariable Long tagId) {
        tagService.removeDocumentTag(documentId, tagId);
        return ApiResponse.success();
    }

    @GetMapping("/documents/{documentId}/tags")
    public ApiResponse<List<Tag>> getDocumentTags(@PathVariable Long documentId) {
        return ApiResponse.success(tagService.getDocumentTags(documentId));
    }
}
