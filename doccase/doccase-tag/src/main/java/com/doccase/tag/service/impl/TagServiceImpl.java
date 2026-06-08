package com.doccase.tag.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.doccase.common.constant.MqConstants;
import com.doccase.common.exception.BizException;
import com.doccase.tag.domain.entity.DocumentTag;
import com.doccase.tag.domain.entity.Tag;
import com.doccase.tag.domain.vo.TagCreateRequest;
import com.doccase.tag.domain.vo.TagMergeRequest;
import com.doccase.tag.domain.vo.TagTreeVO;
import com.doccase.tag.mapper.DocumentTagMapper;
import com.doccase.tag.mapper.TagMapper;
import com.doccase.tag.service.TagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TagServiceImpl implements TagService {

    private final TagMapper tagMapper;
    private final DocumentTagMapper documentTagMapper;
    private final RabbitTemplate rabbitTemplate;

    @Override
    @Transactional
    public Tag createTag(Long userId, TagCreateRequest request) {
        Tag tag = new Tag();
        tag.setName(request.getName());
        tag.setParentId(request.getParentId());
        tag.setColor(request.getColor());
        tag.setIcon(request.getIcon());
        tag.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0);
        tag.setDocumentCount(0);
        tag.setCreatedBy(userId);
        tag.setCreatedAt(LocalDateTime.now());
        tag.setUpdatedAt(LocalDateTime.now());
        tag.setIsDeleted(0);

        // Calculate level and path
        if (request.getParentId() != null && request.getParentId() > 0) {
            Tag parent = tagMapper.selectById(request.getParentId());
            if (parent == null) {
                throw new BizException("Parent tag not found");
            }
            tag.setLevel(parent.getLevel() + 1);
            // Insert first to get ID, then update path
            tagMapper.insert(tag);
            tag.setPath(parent.getPath() + tag.getId() + "/");
        } else {
            tag.setParentId(0L);
            tag.setLevel(1);
            tagMapper.insert(tag);
            tag.setPath("/" + tag.getId() + "/");
        }

        tagMapper.updateById(tag);

        // Publish event
        Map<String, Object> event = new HashMap<>();
        event.put("tagId", tag.getId());
        event.put("action", "created");
        rabbitTemplate.convertAndSend(MqConstants.EXCHANGE_TAG, MqConstants.RK_TAG_UPDATED, event);

        return tag;
    }

    @Override
    @Transactional
    public Tag updateTag(Long tagId, TagCreateRequest request) {
        Tag tag = tagMapper.selectById(tagId);
        if (tag == null) {
            throw new BizException("Tag not found");
        }

        tag.setName(request.getName());
        if (request.getColor() != null) {
            tag.setColor(request.getColor());
        }
        if (request.getIcon() != null) {
            tag.setIcon(request.getIcon());
        }
        if (request.getSortOrder() != null) {
            tag.setSortOrder(request.getSortOrder());
        }
        tag.setUpdatedAt(LocalDateTime.now());

        tagMapper.updateById(tag);

        // Publish event
        Map<String, Object> event = new HashMap<>();
        event.put("tagId", tag.getId());
        event.put("action", "updated");
        rabbitTemplate.convertAndSend(MqConstants.EXCHANGE_TAG, MqConstants.RK_TAG_UPDATED, event);

        return tag;
    }

    @Override
    @Transactional
    public void deleteTag(Long tagId) {
        Tag tag = tagMapper.selectById(tagId);
        if (tag == null) {
            throw new BizException("Tag not found");
        }

        // Delete subtree: all tags whose path starts with this tag's path
        LambdaQueryWrapper<Tag> subtreeQuery = new LambdaQueryWrapper<>();
        subtreeQuery.likeRight(Tag::getPath, tag.getPath());
        List<Tag> subtreeTags = tagMapper.selectList(subtreeQuery);

        List<Long> subtreeTagIds = subtreeTags.stream().map(Tag::getId).collect(Collectors.toList());

        // Soft delete all subtree tags
        for (Tag subtreeTag : subtreeTags) {
            subtreeTag.setIsDeleted(1);
            subtreeTag.setDeletedAt(LocalDateTime.now());
            tagMapper.updateById(subtreeTag);
        }

        // Remove document-tag associations for deleted tags
        if (!subtreeTagIds.isEmpty()) {
            LambdaQueryWrapper<DocumentTag> dtQuery = new LambdaQueryWrapper<>();
            dtQuery.in(DocumentTag::getTagId, subtreeTagIds);
            documentTagMapper.delete(dtQuery);
        }

        // Publish event
        Map<String, Object> event = new HashMap<>();
        event.put("tagIds", subtreeTagIds);
        event.put("action", "deleted");
        rabbitTemplate.convertAndSend(MqConstants.EXCHANGE_TAG, MqConstants.RK_TAG_UPDATED, event);
    }

    @Override
    public List<TagTreeVO> getTagTree() {
        LambdaQueryWrapper<Tag> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.orderByAsc(Tag::getLevel).orderByAsc(Tag::getSortOrder);
        List<Tag> allTags = tagMapper.selectList(queryWrapper);

        return buildTree(allTags, 0L);
    }

    private List<TagTreeVO> buildTree(List<Tag> tags, Long parentId) {
        return tags.stream()
                .filter(tag -> Objects.equals(tag.getParentId(), parentId))
                .map(tag -> {
                    TagTreeVO vo = new TagTreeVO();
                    vo.setId(tag.getId());
                    vo.setName(tag.getName());
                    vo.setParentId(tag.getParentId());
                    vo.setPath(tag.getPath());
                    vo.setLevel(tag.getLevel());
                    vo.setColor(tag.getColor());
                    vo.setIcon(tag.getIcon());
                    vo.setDocumentCount(tag.getDocumentCount());
                    vo.setChildren(buildTree(tags, tag.getId()));
                    return vo;
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void mergeTag(TagMergeRequest request) {
        Tag sourceTag = tagMapper.selectById(request.getSourceTagId());
        Tag targetTag = tagMapper.selectById(request.getTargetTagId());

        if (sourceTag == null || targetTag == null) {
            throw new BizException("Source or target tag not found");
        }

        // Move all document-tag records from source to target
        LambdaUpdateWrapper<DocumentTag> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(DocumentTag::getTagId, request.getSourceTagId())
                .set(DocumentTag::getTagId, request.getTargetTagId());
        documentTagMapper.update(null, updateWrapper);

        // Update target document count
        LambdaQueryWrapper<DocumentTag> countQuery = new LambdaQueryWrapper<>();
        countQuery.eq(DocumentTag::getTagId, request.getTargetTagId());
        long newCount = documentTagMapper.selectCount(countQuery);
        targetTag.setDocumentCount((int) newCount);
        targetTag.setUpdatedAt(LocalDateTime.now());
        tagMapper.updateById(targetTag);

        // Soft delete source tag
        sourceTag.setIsDeleted(1);
        sourceTag.setDeletedAt(LocalDateTime.now());
        tagMapper.updateById(sourceTag);

        // Publish merge event
        Map<String, Object> event = new HashMap<>();
        event.put("sourceTagId", request.getSourceTagId());
        event.put("targetTagId", request.getTargetTagId());
        event.put("action", "merged");
        rabbitTemplate.convertAndSend(MqConstants.EXCHANGE_TAG, MqConstants.RK_TAG_MERGED, event);
    }

    @Override
    @Transactional
    public void importTags(List<TagCreateRequest> tags, Long userId) {
        for (TagCreateRequest request : tags) {
            createTag(userId, request);
        }
    }

    @Override
    public List<Tag> exportTags() {
        LambdaQueryWrapper<Tag> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.orderByAsc(Tag::getLevel).orderByAsc(Tag::getSortOrder);
        return tagMapper.selectList(queryWrapper);
    }

    @Override
    @Transactional
    public void addDocumentTag(Long documentId, Long tagId) {
        // Check if already exists
        LambdaQueryWrapper<DocumentTag> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(DocumentTag::getDocumentId, documentId)
                .eq(DocumentTag::getTagId, tagId);
        if (documentTagMapper.selectCount(queryWrapper) > 0) {
            return;
        }

        DocumentTag documentTag = new DocumentTag();
        documentTag.setDocumentId(documentId);
        documentTag.setTagId(tagId);
        documentTag.setCreatedAt(LocalDateTime.now());
        documentTagMapper.insert(documentTag);

        // Update document count
        Tag tag = tagMapper.selectById(tagId);
        if (tag != null) {
            tag.setDocumentCount(tag.getDocumentCount() + 1);
            tag.setUpdatedAt(LocalDateTime.now());
            tagMapper.updateById(tag);
        }
    }

    @Override
    @Transactional
    public void removeDocumentTag(Long documentId, Long tagId) {
        LambdaQueryWrapper<DocumentTag> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(DocumentTag::getDocumentId, documentId)
                .eq(DocumentTag::getTagId, tagId);
        int deleted = documentTagMapper.delete(queryWrapper);

        if (deleted > 0) {
            // Update document count
            Tag tag = tagMapper.selectById(tagId);
            if (tag != null && tag.getDocumentCount() > 0) {
                tag.setDocumentCount(tag.getDocumentCount() - 1);
                tag.setUpdatedAt(LocalDateTime.now());
                tagMapper.updateById(tag);
            }
        }
    }

    @Override
    public List<Tag> getDocumentTags(Long documentId) {
        LambdaQueryWrapper<DocumentTag> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(DocumentTag::getDocumentId, documentId);
        List<DocumentTag> documentTags = documentTagMapper.selectList(queryWrapper);

        if (documentTags.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> tagIds = documentTags.stream()
                .map(DocumentTag::getTagId)
                .collect(Collectors.toList());

        return tagMapper.selectBatchIds(tagIds);
    }
}
