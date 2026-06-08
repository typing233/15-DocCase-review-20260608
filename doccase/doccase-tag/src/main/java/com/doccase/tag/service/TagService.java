package com.doccase.tag.service;

import com.doccase.tag.domain.entity.Tag;
import com.doccase.tag.domain.vo.*;

import java.util.List;

public interface TagService {

    Tag createTag(String tenantId, Long userId, TagCreateRequest request);

    Tag updateTag(String tenantId, Long tagId, TagCreateRequest request);

    void deleteTag(String tenantId, Long tagId);

    List<TagTreeVO> getTagTree(String tenantId);

    void mergeTag(String tenantId, Long operatorId, TagMergeRequest request);

    void importTags(String tenantId, List<TagCreateRequest> tags, Long userId);

    List<Tag> exportTags(String tenantId);

    void addDocumentTag(String tenantId, Long documentId, Long tagId);

    void removeDocumentTag(String tenantId, Long documentId, Long tagId);

    List<Tag> getDocumentTags(String tenantId, Long documentId);

    List<Tag> getDocumentTagsWithInheritance(String tenantId, Long documentId);

    void batchTag(String tenantId, Long operatorId, BatchTagRequest request);

    void batchMove(String tenantId, Long operatorId, BatchMoveRequest request);

    void batchDelete(String tenantId, Long operatorId, BatchDeleteRequest request);

    List<Long> getInheritedDocumentIds(String tenantId, Long tagId);

    // Legacy support without tenantId
    default Tag createTag(Long userId, TagCreateRequest request) {
        return createTag("default", userId, request);
    }

    default Tag updateTag(Long tagId, TagCreateRequest request) {
        return updateTag("default", tagId, request);
    }

    default void deleteTag(Long tagId) {
        deleteTag("default", tagId);
    }

    default List<TagTreeVO> getTagTree() {
        return getTagTree("default");
    }

    default void mergeTag(TagMergeRequest request) {
        mergeTag("default", 0L, request);
    }

    default void importTags(List<TagCreateRequest> tags, Long userId) {
        importTags("default", tags, userId);
    }

    default List<Tag> exportTags() {
        return exportTags("default");
    }

    default void addDocumentTag(Long documentId, Long tagId) {
        addDocumentTag("default", documentId, tagId);
    }

    default void removeDocumentTag(Long documentId, Long tagId) {
        removeDocumentTag("default", documentId, tagId);
    }

    default List<Tag> getDocumentTags(Long documentId) {
        return getDocumentTags("default", documentId);
    }
}
