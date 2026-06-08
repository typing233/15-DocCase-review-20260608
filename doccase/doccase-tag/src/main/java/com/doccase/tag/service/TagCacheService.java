package com.doccase.tag.service;

import com.doccase.tag.domain.vo.TagTreeVO;

import java.util.List;

public interface TagCacheService {

    List<TagTreeVO> getCachedTagTree(String tenantId);

    void invalidateTagTree(String tenantId);

    void invalidateAllTagTrees();

    boolean tagExists(String tenantId, Long tagId);

    void addTagToBloom(String tenantId, Long tagId);
}
