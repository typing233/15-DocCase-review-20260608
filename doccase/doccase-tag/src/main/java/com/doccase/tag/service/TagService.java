package com.doccase.tag.service;

import com.doccase.tag.domain.entity.Tag;
import com.doccase.tag.domain.vo.TagCreateRequest;
import com.doccase.tag.domain.vo.TagMergeRequest;
import com.doccase.tag.domain.vo.TagTreeVO;

import java.util.List;

public interface TagService {

    Tag createTag(Long userId, TagCreateRequest request);

    Tag updateTag(Long tagId, TagCreateRequest request);

    void deleteTag(Long tagId);

    List<TagTreeVO> getTagTree();

    void mergeTag(TagMergeRequest request);

    void importTags(List<TagCreateRequest> tags, Long userId);

    List<Tag> exportTags();

    void addDocumentTag(Long documentId, Long tagId);

    void removeDocumentTag(Long documentId, Long tagId);

    List<Tag> getDocumentTags(Long documentId);
}
