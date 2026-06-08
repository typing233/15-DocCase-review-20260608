package com.doccase.tag.listener;

import com.doccase.common.constant.MqConstants;
import com.doccase.tag.domain.entity.DocumentTag;
import com.doccase.tag.domain.entity.Tag;
import com.doccase.tag.mapper.DocumentTagMapper;
import com.doccase.tag.mapper.TagMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class TagIndexUpdateListener {

    private final TagMapper tagMapper;
    private final DocumentTagMapper documentTagMapper;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "tag.document.count.queue", durable = "true"),
            exchange = @Exchange(value = MqConstants.EXCHANGE_DOCUMENT, type = "topic"),
            key = {MqConstants.RK_DOCUMENT_CREATED}
    ))
    public void onDocumentCreated(Map<String, Object> message) {
        try {
            log.info("Received document.created event for tag update: {}", message);
            Object tagIdsObj = message.get("tagIds");
            if (tagIdsObj instanceof List<?> tagIds) {
                for (Object tagIdObj : tagIds) {
                    Long tagId = Long.valueOf(tagIdObj.toString());
                    recalculateDocumentCount(tagId);
                }
            }
        } catch (Exception e) {
            log.error("Error processing document.created event for tag count update", e);
        }
    }

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "tag.document.delete.queue", durable = "true"),
            exchange = @Exchange(value = MqConstants.EXCHANGE_DOCUMENT, type = "topic"),
            key = {MqConstants.RK_DOCUMENT_DELETED}
    ))
    public void onDocumentDeleted(Map<String, Object> message) {
        try {
            log.info("Received document.deleted event for tag update: {}", message);
            Object documentIdObj = message.get("documentId");
            if (documentIdObj != null) {
                Long documentId = Long.valueOf(documentIdObj.toString());
                // Find all tags associated with this document and update counts
                LambdaQueryWrapper<DocumentTag> queryWrapper = new LambdaQueryWrapper<>();
                queryWrapper.eq(DocumentTag::getDocumentId, documentId);
                List<DocumentTag> documentTags = documentTagMapper.selectList(queryWrapper);

                for (DocumentTag dt : documentTags) {
                    recalculateDocumentCount(dt.getTagId());
                }

                // Remove document-tag associations
                documentTagMapper.delete(queryWrapper);
            }
        } catch (Exception e) {
            log.error("Error processing document.deleted event for tag count update", e);
        }
    }

    private void recalculateDocumentCount(Long tagId) {
        LambdaQueryWrapper<DocumentTag> countQuery = new LambdaQueryWrapper<>();
        countQuery.eq(DocumentTag::getTagId, tagId);
        long count = documentTagMapper.selectCount(countQuery);

        Tag tag = tagMapper.selectById(tagId);
        if (tag != null) {
            tag.setDocumentCount((int) count);
            tagMapper.updateById(tag);
        }
    }
}
