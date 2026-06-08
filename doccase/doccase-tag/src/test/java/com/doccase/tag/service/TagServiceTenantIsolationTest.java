package com.doccase.tag.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.doccase.common.exception.BizException;
import com.doccase.common.util.DistributedLockUtil;
import com.doccase.tag.domain.entity.DocumentTag;
import com.doccase.tag.domain.entity.Tag;
import com.doccase.tag.domain.vo.*;
import com.doccase.tag.mapper.DocumentTagMapper;
import com.doccase.tag.mapper.TagMapper;
import com.doccase.tag.mapper.TagOperationLogMapper;
import com.doccase.tag.service.impl.TagCacheServiceImpl;
import com.doccase.tag.service.impl.TagServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tag Service - Tenant Isolation Tests")
class TagServiceTenantIsolationTest {

    @Mock private TagMapper tagMapper;
    @Mock private DocumentTagMapper documentTagMapper;
    @Mock private TagOperationLogMapper operationLogMapper;
    @Mock private RabbitTemplate rabbitTemplate;
    @Mock private TagCacheServiceImpl tagCacheService;
    @Mock private DistributedLockUtil distributedLockUtil;
    @Mock private ObjectMapper objectMapper;

    @InjectMocks
    private TagServiceImpl tagService;

    private Tag tenantATag;
    private Tag tenantBTag;

    @BeforeEach
    void setup() {
        tenantATag = new Tag();
        tenantATag.setId(1L);
        tenantATag.setTenantId("tenant-A");
        tenantATag.setName("TagA");
        tenantATag.setPath("/1/");
        tenantATag.setLevel(1);
        tenantATag.setDocumentCount(5);
        tenantATag.setIsDeleted(0);

        tenantBTag = new Tag();
        tenantBTag.setId(2L);
        tenantBTag.setTenantId("tenant-B");
        tenantBTag.setName("TagB");
        tenantBTag.setPath("/2/");
        tenantBTag.setLevel(1);
        tenantBTag.setDocumentCount(3);
        tenantBTag.setIsDeleted(0);
    }

    @Test
    @DisplayName("updateTag - should reject when tag belongs to different tenant")
    void updateTag_crossTenant_throws() {
        when(tagMapper.selectById(1L)).thenReturn(tenantATag);

        TagCreateRequest request = new TagCreateRequest();
        request.setName("Hacked");

        BizException ex = assertThrows(BizException.class, () ->
                tagService.updateTag("tenant-B", 1L, request));

        assertTrue(ex.getMessage().contains("does not belong to current tenant"));
        verify(tagMapper, never()).updateById(any());
    }

    @Test
    @DisplayName("updateTag - should succeed when tag belongs to same tenant")
    void updateTag_sameTenant_succeeds() {
        when(tagMapper.selectById(1L)).thenReturn(tenantATag);
        when(tagMapper.updateById(any())).thenReturn(1);

        TagCreateRequest request = new TagCreateRequest();
        request.setName("Updated");

        Tag result = tagService.updateTag("tenant-A", 1L, request);
        assertEquals("Updated", result.getName());
    }

    @Test
    @DisplayName("deleteTag - should reject when tag belongs to different tenant")
    void deleteTag_crossTenant_throws() {
        when(tagMapper.selectById(2L)).thenReturn(tenantBTag);

        BizException ex = assertThrows(BizException.class, () ->
                tagService.deleteTag("tenant-A", 2L));

        assertTrue(ex.getMessage().contains("does not belong to current tenant"));
    }

    @Test
    @DisplayName("addDocumentTag - should reject cross-tenant tag assignment")
    void addDocumentTag_crossTenant_throws() {
        when(tagMapper.selectById(2L)).thenReturn(tenantBTag);

        BizException ex = assertThrows(BizException.class, () ->
                tagService.addDocumentTag("tenant-A", 100L, 2L));

        assertTrue(ex.getMessage().contains("does not belong to current tenant"));
        verify(documentTagMapper, never()).insert(any());
    }

    @Test
    @DisplayName("removeDocumentTag - should reject cross-tenant tag removal")
    void removeDocumentTag_crossTenant_throws() {
        when(tagMapper.selectById(2L)).thenReturn(tenantBTag);

        BizException ex = assertThrows(BizException.class, () ->
                tagService.removeDocumentTag("tenant-A", 100L, 2L));

        assertTrue(ex.getMessage().contains("does not belong to current tenant"));
    }

    @Test
    @DisplayName("createTag - should reject when parent belongs to different tenant")
    void createTag_crossTenantParent_throws() {
        when(tagMapper.selectById(2L)).thenReturn(tenantBTag);

        TagCreateRequest request = new TagCreateRequest();
        request.setName("Child");
        request.setParentId(2L);

        BizException ex = assertThrows(BizException.class, () ->
                tagService.createTag("tenant-A", 1L, request));

        assertTrue(ex.getMessage().contains("does not belong to current tenant"));
    }

    @Test
    @DisplayName("getInheritedDocumentIds - should reject cross-tenant access")
    void getInheritedDocumentIds_crossTenant_throws() {
        when(tagMapper.selectById(1L)).thenReturn(tenantATag);

        BizException ex = assertThrows(BizException.class, () ->
                tagService.getInheritedDocumentIds("tenant-B", 1L));

        assertTrue(ex.getMessage().contains("does not belong to current tenant"));
    }

    @Test
    @DisplayName("mergeTag - should reject when source tag belongs to different tenant")
    void mergeTag_crossTenantSource_throws() {
        // Setup distributedLockUtil to execute the runnable immediately
        doAnswer(invocation -> {
            Runnable runnable = invocation.getArgument(4);
            runnable.run();
            return null;
        }).when(distributedLockUtil).executeWithLock(anyString(), anyLong(), anyLong(), any(TimeUnit.class), any(Runnable.class));

        when(tagMapper.selectById(2L)).thenReturn(tenantBTag);

        TagMergeRequest request = new TagMergeRequest();
        request.setSourceTagId(2L);
        request.setTargetTagId(1L);

        BizException ex = assertThrows(BizException.class, () ->
                tagService.mergeTag("tenant-A", 1L, request));

        assertTrue(ex.getMessage().contains("does not belong to current tenant"));
    }

    @Test
    @DisplayName("batchMove - should reject when any tag belongs to different tenant")
    void batchMove_crossTenant_throws() {
        doAnswer(invocation -> {
            Runnable runnable = invocation.getArgument(4);
            runnable.run();
            return null;
        }).when(distributedLockUtil).executeWithLock(anyString(), anyLong(), anyLong(), any(TimeUnit.class), any(Runnable.class));

        when(tagMapper.selectById(2L)).thenReturn(tenantBTag);

        BatchMoveRequest request = new BatchMoveRequest();
        request.setTagIds(List.of(2L));
        request.setTargetParentId(null);

        BizException ex = assertThrows(BizException.class, () ->
                tagService.batchMove("tenant-A", 1L, request));

        assertTrue(ex.getMessage().contains("does not belong to current tenant"));
    }

    @Test
    @DisplayName("batchTag - should reject when any tagId belongs to different tenant")
    void batchTag_crossTenant_throws() {
        when(tagMapper.selectById(2L)).thenReturn(tenantBTag);

        BatchTagRequest request = new BatchTagRequest();
        request.setDocumentIds(List.of(100L));
        request.setTagIds(List.of(2L));
        request.setAction(BatchTagRequest.Action.ADD);

        BizException ex = assertThrows(BizException.class, () ->
                tagService.batchTag("tenant-A", 1L, request));

        assertTrue(ex.getMessage().contains("does not belong to current tenant"));
    }

    @Test
    @DisplayName("Tag not found - should throw clear error")
    void tagNotFound_throws() {
        when(tagMapper.selectById(999L)).thenReturn(null);

        BizException ex = assertThrows(BizException.class, () ->
                tagService.deleteTag("tenant-A", 999L));

        assertTrue(ex.getMessage().contains("Tag not found"));
    }
}
