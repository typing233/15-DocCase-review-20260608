package com.doccase.tag.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.doccase.common.constant.MqConstants;
import com.doccase.common.constant.RedisKeyConstants;
import com.doccase.common.exception.BizException;
import com.doccase.common.util.DistributedLockUtil;
import com.doccase.tag.domain.entity.DocumentTag;
import com.doccase.tag.domain.entity.Tag;
import com.doccase.tag.domain.entity.TagOperationLog;
import com.doccase.tag.domain.vo.*;
import com.doccase.tag.mapper.DocumentTagMapper;
import com.doccase.tag.mapper.TagMapper;
import com.doccase.tag.mapper.TagOperationLogMapper;
import com.doccase.tag.service.TagCacheService;
import com.doccase.tag.service.TagService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TagServiceImpl implements TagService {

    private final TagMapper tagMapper;
    private final DocumentTagMapper documentTagMapper;
    private final TagOperationLogMapper operationLogMapper;
    private final RabbitTemplate rabbitTemplate;
    private final TagCacheServiceImpl tagCacheService;
    private final DistributedLockUtil distributedLockUtil;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public Tag createTag(String tenantId, Long userId, TagCreateRequest request) {
        Tag tag = new Tag();
        tag.setTenantId(tenantId);
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

        if (request.getParentId() != null && request.getParentId() > 0) {
            Tag parent = getTagAndVerifyTenant(request.getParentId(), tenantId);
            tag.setLevel(parent.getLevel() + 1);
            tagMapper.insert(tag);
            tag.setPath(parent.getPath() + tag.getId() + "/");
        } else {
            tag.setParentId(0L);
            tag.setLevel(1);
            tagMapper.insert(tag);
            tag.setPath("/" + tag.getId() + "/");
        }

        tagMapper.updateById(tag);

        tagCacheService.invalidateTagTree(tenantId);
        tagCacheService.addTagToBloom(tenantId, tag.getId());

        Map<String, Object> event = new HashMap<>();
        event.put("tagId", tag.getId());
        event.put("tenantId", tenantId);
        event.put("action", "created");
        rabbitTemplate.convertAndSend(MqConstants.EXCHANGE_TAG, MqConstants.RK_TAG_UPDATED, event);

        return tag;
    }

    @Override
    @Transactional
    public Tag updateTag(String tenantId, Long tagId, TagCreateRequest request) {
        Tag tag = getTagAndVerifyTenant(tagId, tenantId);

        tag.setName(request.getName());
        if (request.getColor() != null) tag.setColor(request.getColor());
        if (request.getIcon() != null) tag.setIcon(request.getIcon());
        if (request.getSortOrder() != null) tag.setSortOrder(request.getSortOrder());
        tag.setUpdatedAt(LocalDateTime.now());

        tagMapper.updateById(tag);
        tagCacheService.invalidateTagTree(tenantId);

        Map<String, Object> event = new HashMap<>();
        event.put("tagId", tag.getId());
        event.put("tenantId", tenantId);
        event.put("action", "updated");
        rabbitTemplate.convertAndSend(MqConstants.EXCHANGE_TAG, MqConstants.RK_TAG_UPDATED, event);

        return tag;
    }

    @Override
    @Transactional
    public void deleteTag(String tenantId, Long tagId) {
        Tag tag = getTagAndVerifyTenant(tagId, tenantId);

        LambdaQueryWrapper<Tag> subtreeQuery = new LambdaQueryWrapper<>();
        subtreeQuery.eq(Tag::getTenantId, tenantId)
                .likeRight(Tag::getPath, tag.getPath());
        List<Tag> subtreeTags = tagMapper.selectList(subtreeQuery);

        List<Long> subtreeTagIds = subtreeTags.stream().map(Tag::getId).collect(Collectors.toList());

        for (Tag subtreeTag : subtreeTags) {
            subtreeTag.setIsDeleted(1);
            subtreeTag.setDeletedAt(LocalDateTime.now());
            tagMapper.updateById(subtreeTag);
        }

        if (!subtreeTagIds.isEmpty()) {
            LambdaQueryWrapper<DocumentTag> dtQuery = new LambdaQueryWrapper<>();
            dtQuery.eq(DocumentTag::getTenantId, tenantId)
                    .in(DocumentTag::getTagId, subtreeTagIds);
            documentTagMapper.delete(dtQuery);
        }

        tagCacheService.invalidateTagTree(tenantId);

        Map<String, Object> event = new HashMap<>();
        event.put("tagIds", subtreeTagIds);
        event.put("tenantId", tenantId);
        event.put("action", "deleted");
        rabbitTemplate.convertAndSend(MqConstants.EXCHANGE_TAG, MqConstants.RK_TAG_UPDATED, event);
    }

    @Override
    public List<TagTreeVO> getTagTree(String tenantId) {
        List<TagTreeVO> cached = tagCacheService.getCachedTagTree(tenantId);
        if (cached != null) return cached;

        LambdaQueryWrapper<Tag> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Tag::getTenantId, tenantId)
                .orderByAsc(Tag::getLevel)
                .orderByAsc(Tag::getSortOrder);
        List<Tag> allTags = tagMapper.selectList(queryWrapper);

        List<TagTreeVO> tree = buildTree(allTags, 0L);
        tagCacheService.cacheTagTree(tenantId, tree);
        return tree;
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
    public void mergeTag(String tenantId, Long operatorId, TagMergeRequest request) {
        String lockKey = RedisKeyConstants.TAG_LOCK_PREFIX + request.getSourceTagId() + ":" + request.getTargetTagId();
        distributedLockUtil.executeWithLock(lockKey, 10, 30, TimeUnit.SECONDS, () -> {
            Tag sourceTag = getTagAndVerifyTenant(request.getSourceTagId(), tenantId);
            Tag targetTag = getTagAndVerifyTenant(request.getTargetTagId(), tenantId);

            // Detect conflicts: documents that already have the target tag
            LambdaQueryWrapper<DocumentTag> sourceDocsQuery = new LambdaQueryWrapper<>();
            sourceDocsQuery.eq(DocumentTag::getTenantId, tenantId)
                    .eq(DocumentTag::getTagId, request.getSourceTagId());
            List<DocumentTag> sourceDocs = documentTagMapper.selectList(sourceDocsQuery);

            List<Long> sourceDocIds = sourceDocs.stream().map(DocumentTag::getDocumentId).toList();

            if (!sourceDocIds.isEmpty()) {
                LambdaQueryWrapper<DocumentTag> conflictQuery = new LambdaQueryWrapper<>();
                conflictQuery.eq(DocumentTag::getTenantId, tenantId)
                        .eq(DocumentTag::getTagId, request.getTargetTagId())
                        .in(DocumentTag::getDocumentId, sourceDocIds);
                List<DocumentTag> conflicts = documentTagMapper.selectList(conflictQuery);
                Set<Long> conflictDocIds = conflicts.stream().map(DocumentTag::getDocumentId).collect(Collectors.toSet());

                // Auto-resolve: remove duplicates from source before merge
                if (!conflictDocIds.isEmpty()) {
                    LambdaQueryWrapper<DocumentTag> removeConflicts = new LambdaQueryWrapper<>();
                    removeConflicts.eq(DocumentTag::getTenantId, tenantId)
                            .eq(DocumentTag::getTagId, request.getSourceTagId())
                            .in(DocumentTag::getDocumentId, conflictDocIds);
                    documentTagMapper.delete(removeConflicts);
                }
            }

            // Move remaining associations from source to target
            LambdaUpdateWrapper<DocumentTag> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(DocumentTag::getTenantId, tenantId)
                    .eq(DocumentTag::getTagId, request.getSourceTagId())
                    .set(DocumentTag::getTagId, request.getTargetTagId());
            documentTagMapper.update(null, updateWrapper);

            // Recalculate target count
            LambdaQueryWrapper<DocumentTag> countQuery = new LambdaQueryWrapper<>();
            countQuery.eq(DocumentTag::getTenantId, tenantId)
                    .eq(DocumentTag::getTagId, request.getTargetTagId());
            long newCount = documentTagMapper.selectCount(countQuery);
            targetTag.setDocumentCount((int) newCount);
            targetTag.setUpdatedAt(LocalDateTime.now());
            tagMapper.updateById(targetTag);

            // Soft delete source tag
            sourceTag.setIsDeleted(1);
            sourceTag.setDeletedAt(LocalDateTime.now());
            tagMapper.updateById(sourceTag);

            // Log the operation
            TagOperationLog opLog = new TagOperationLog();
            opLog.setTenantId(tenantId);
            opLog.setOperationType("MERGE");
            opLog.setSourceTagIds("[" + request.getSourceTagId() + "]");
            opLog.setTargetTagId(request.getTargetTagId());
            opLog.setOperatorId(operatorId);
            opLog.setStatus(2);
            opLog.setCreatedAt(LocalDateTime.now());
            opLog.setCompletedAt(LocalDateTime.now());
            operationLogMapper.insert(opLog);

            tagCacheService.invalidateTagTree(tenantId);

            Map<String, Object> event = new HashMap<>();
            event.put("sourceTagId", request.getSourceTagId());
            event.put("targetTagId", request.getTargetTagId());
            event.put("tenantId", tenantId);
            event.put("affectedDocumentIds", sourceDocIds);
            event.put("action", "merged");
            rabbitTemplate.convertAndSend(MqConstants.EXCHANGE_TAG, MqConstants.RK_TAG_MERGED, event);
        });
    }

    @Override
    @Transactional
    public void importTags(String tenantId, List<TagCreateRequest> tags, Long userId) {
        for (TagCreateRequest request : tags) {
            createTag(tenantId, userId, request);
        }
    }

    @Override
    public List<Tag> exportTags(String tenantId) {
        LambdaQueryWrapper<Tag> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Tag::getTenantId, tenantId)
                .orderByAsc(Tag::getLevel)
                .orderByAsc(Tag::getSortOrder);
        return tagMapper.selectList(queryWrapper);
    }

    @Override
    @Transactional
    public void addDocumentTag(String tenantId, Long documentId, Long tagId) {
        getTagAndVerifyTenant(tagId, tenantId);

        LambdaQueryWrapper<DocumentTag> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(DocumentTag::getTenantId, tenantId)
                .eq(DocumentTag::getDocumentId, documentId)
                .eq(DocumentTag::getTagId, tagId);
        if (documentTagMapper.selectCount(queryWrapper) > 0) return;

        DocumentTag documentTag = new DocumentTag();
        documentTag.setTenantId(tenantId);
        documentTag.setDocumentId(documentId);
        documentTag.setTagId(tagId);
        documentTag.setCreatedAt(LocalDateTime.now());
        documentTagMapper.insert(documentTag);

        tagMapper.update(null, new LambdaUpdateWrapper<Tag>()
                .eq(Tag::getId, tagId)
                .eq(Tag::getTenantId, tenantId)
                .setSql("document_count = document_count + 1")
                .set(Tag::getUpdatedAt, LocalDateTime.now()));
    }

    @Override
    @Transactional
    public void removeDocumentTag(String tenantId, Long documentId, Long tagId) {
        getTagAndVerifyTenant(tagId, tenantId);

        LambdaQueryWrapper<DocumentTag> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(DocumentTag::getTenantId, tenantId)
                .eq(DocumentTag::getDocumentId, documentId)
                .eq(DocumentTag::getTagId, tagId);
        int deleted = documentTagMapper.delete(queryWrapper);

        if (deleted > 0) {
            tagMapper.update(null, new LambdaUpdateWrapper<Tag>()
                    .eq(Tag::getId, tagId)
                    .eq(Tag::getTenantId, tenantId)
                    .gt(Tag::getDocumentCount, 0)
                    .setSql("document_count = document_count - 1")
                    .set(Tag::getUpdatedAt, LocalDateTime.now()));
        }
    }

    @Override
    public List<Tag> getDocumentTags(String tenantId, Long documentId) {
        LambdaQueryWrapper<DocumentTag> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(DocumentTag::getTenantId, tenantId)
                .eq(DocumentTag::getDocumentId, documentId);
        List<DocumentTag> documentTags = documentTagMapper.selectList(queryWrapper);

        if (documentTags.isEmpty()) return Collections.emptyList();

        List<Long> tagIds = documentTags.stream().map(DocumentTag::getTagId).collect(Collectors.toList());
        return tagMapper.selectBatchIds(tagIds);
    }

    @Override
    public List<Tag> getDocumentTagsWithInheritance(String tenantId, Long documentId) {
        List<Tag> directTags = getDocumentTags(tenantId, documentId);
        if (directTags.isEmpty()) return Collections.emptyList();

        Set<Long> allTagIds = new HashSet<>();
        for (Tag tag : directTags) {
            allTagIds.add(tag.getId());
            // Add all ancestor tags from materialized path
            if (tag.getPath() != null) {
                String[] parts = tag.getPath().split("/");
                for (String part : parts) {
                    if (!part.isEmpty()) {
                        allTagIds.add(Long.parseLong(part));
                    }
                }
            }
        }

        return tagMapper.selectBatchIds(new ArrayList<>(allTagIds));
    }

    @Override
    @Transactional
    public void batchTag(String tenantId, Long operatorId, BatchTagRequest request) {
        // Pre-validate all tag IDs belong to this tenant
        for (Long tagId : request.getTagIds()) {
            getTagAndVerifyTenant(tagId, tenantId);
        }

        String lockKey = RedisKeyConstants.TAG_BATCH_LOCK_PREFIX + tenantId + ":" + UUID.randomUUID();
        distributedLockUtil.executeWithLock(lockKey, 10, 60, TimeUnit.SECONDS, () -> {
            TagOperationLog opLog = new TagOperationLog();
            opLog.setTenantId(tenantId);
            opLog.setOperationType(request.getAction() == BatchTagRequest.Action.ADD ? "BATCH_TAG" : "BATCH_UNTAG");
            opLog.setOperatorId(operatorId);
            opLog.setStatus(1);
            opLog.setCreatedAt(LocalDateTime.now());

            try {
                opLog.setSourceTagIds(objectMapper.writeValueAsString(request.getTagIds()));
                opLog.setDocumentIds(objectMapper.writeValueAsString(request.getDocumentIds()));
            } catch (Exception ignored) {}

            operationLogMapper.insert(opLog);

            int processed = 0;
            if (request.getAction() == BatchTagRequest.Action.ADD) {
                for (Long docId : request.getDocumentIds()) {
                    for (Long tagId : request.getTagIds()) {
                        addDocumentTag(tenantId, docId, tagId);
                        processed++;
                    }
                }
            } else {
                for (Long docId : request.getDocumentIds()) {
                    for (Long tagId : request.getTagIds()) {
                        removeDocumentTag(tenantId, docId, tagId);
                        processed++;
                    }
                }
            }

            opLog.setStatus(2);
            opLog.setCompletedAt(LocalDateTime.now());
            opLog.setResultDetail("{\"processed\":" + processed + "}");
            operationLogMapper.updateById(opLog);

            Map<String, Object> event = new HashMap<>();
            event.put("tenantId", tenantId);
            event.put("action", request.getAction().name());
            event.put("documentIds", request.getDocumentIds());
            event.put("tagIds", request.getTagIds());
            rabbitTemplate.convertAndSend(MqConstants.EXCHANGE_TAG, MqConstants.RK_TAG_BATCH_COMPLETED, event);
        });
    }

    @Override
    @Transactional
    public void batchMove(String tenantId, Long operatorId, BatchMoveRequest request) {
        String lockKey = RedisKeyConstants.TAG_BATCH_LOCK_PREFIX + tenantId + ":move";
        distributedLockUtil.executeWithLock(lockKey, 10, 60, TimeUnit.SECONDS, () -> {
            Long targetParentId = request.getTargetParentId() != null ? request.getTargetParentId() : 0L;
            Tag parentTag = null;

            if (targetParentId > 0) {
                parentTag = getTagAndVerifyTenant(targetParentId, tenantId);
            }

            for (Long tagId : request.getTagIds()) {
                Tag tag = getTagAndVerifyTenant(tagId, tenantId);

                // Detect circular reference
                if (parentTag != null && parentTag.getPath().contains("/" + tagId + "/")) {
                    throw new BizException("Cannot move tag to its own subtree (circular reference)");
                }

                String oldPath = tag.getPath();
                tag.setParentId(targetParentId);
                if (parentTag != null) {
                    tag.setLevel(parentTag.getLevel() + 1);
                    tag.setPath(parentTag.getPath() + tag.getId() + "/");
                } else {
                    tag.setLevel(1);
                    tag.setPath("/" + tag.getId() + "/");
                }
                tag.setUpdatedAt(LocalDateTime.now());
                tagMapper.updateById(tag);

                // Update subtree paths
                LambdaQueryWrapper<Tag> subtreeQuery = new LambdaQueryWrapper<>();
                subtreeQuery.eq(Tag::getTenantId, tenantId)
                        .likeRight(Tag::getPath, oldPath)
                        .ne(Tag::getId, tagId);
                List<Tag> children = tagMapper.selectList(subtreeQuery);
                for (Tag child : children) {
                    child.setPath(child.getPath().replace(oldPath, tag.getPath()));
                    child.setLevel(tag.getLevel() + (child.getLevel() - tag.getLevel()));
                    tagMapper.updateById(child);
                }
            }

            tagCacheService.invalidateTagTree(tenantId);
        });
    }

    @Override
    @Transactional
    public void batchDelete(String tenantId, Long operatorId, BatchDeleteRequest request) {
        String lockKey = RedisKeyConstants.TAG_BATCH_LOCK_PREFIX + tenantId + ":delete";
        distributedLockUtil.executeWithLock(lockKey, 10, 60, TimeUnit.SECONDS, () -> {
            for (Long tagId : request.getTagIds()) {
                deleteTag(tenantId, tagId);
            }
        });
    }

    @Override
    public List<Long> getInheritedDocumentIds(String tenantId, Long tagId) {
        Tag tag = getTagAndVerifyTenant(tagId, tenantId);

        // Find all descendant tags (using materialized path)
        LambdaQueryWrapper<Tag> subtreeQuery = new LambdaQueryWrapper<>();
        subtreeQuery.eq(Tag::getTenantId, tenantId)
                .likeRight(Tag::getPath, tag.getPath());
        List<Tag> subtreeTags = tagMapper.selectList(subtreeQuery);

        List<Long> allTagIds = subtreeTags.stream().map(Tag::getId).collect(Collectors.toList());
        if (allTagIds.isEmpty()) return Collections.emptyList();

        LambdaQueryWrapper<DocumentTag> docQuery = new LambdaQueryWrapper<>();
        docQuery.eq(DocumentTag::getTenantId, tenantId)
                .in(DocumentTag::getTagId, allTagIds);
        List<DocumentTag> documentTags = documentTagMapper.selectList(docQuery);

        return documentTags.stream()
                .map(DocumentTag::getDocumentId)
                .distinct()
                .collect(Collectors.toList());
    }

    private Tag getTagAndVerifyTenant(Long tagId, String tenantId) {
        Tag tag = tagMapper.selectById(tagId);
        if (tag == null) {
            throw new BizException("Tag not found");
        }
        if (!tenantId.equals(tag.getTenantId())) {
            throw new BizException("Access denied: tag does not belong to current tenant");
        }
        return tag;
    }
}
