package com.doccase.search.listener;

import com.doccase.common.constant.MqConstants;
import com.doccase.search.document.DocumentIndex;
import com.doccase.search.service.SearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class IndexUpdateListener {

    private final SearchService searchService;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConstants.QUEUE_SEARCH_DOCUMENT, durable = "true"),
            exchange = @Exchange(value = MqConstants.EXCHANGE_DOCUMENT, type = "topic"),
            key = {MqConstants.RK_DOCUMENT_CREATED}
    ))
    public void onDocumentCreated(Map<String, Object> message) {
        try {
            log.info("Received document.created event for indexing: {}", message);
            DocumentIndex doc = buildDocumentIndex(message);
            searchService.indexDocument(doc);
        } catch (Exception e) {
            log.error("Failed to index document on created event", e);
        }
    }

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "search.document.update.queue", durable = "true"),
            exchange = @Exchange(value = MqConstants.EXCHANGE_DOCUMENT, type = "topic"),
            key = {MqConstants.RK_DOCUMENT_UPDATED}
    ))
    public void onDocumentUpdated(Map<String, Object> message) {
        try {
            log.info("Received document.updated event for indexing: {}", message);
            DocumentIndex doc = buildDocumentIndex(message);
            searchService.updateDocument(doc);
        } catch (Exception e) {
            log.error("Failed to update document index on updated event", e);
        }
    }

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "search.document.delete.queue", durable = "true"),
            exchange = @Exchange(value = MqConstants.EXCHANGE_DOCUMENT, type = "topic"),
            key = {MqConstants.RK_DOCUMENT_DELETED}
    ))
    public void onDocumentDeleted(Map<String, Object> message) {
        try {
            log.info("Received document.deleted event for indexing: {}", message);
            Object documentIdObj = message.get("documentId");
            if (documentIdObj != null) {
                Long documentId = Long.valueOf(documentIdObj.toString());
                searchService.deleteDocument(documentId);
            }
        } catch (Exception e) {
            log.error("Failed to delete document from index on deleted event", e);
        }
    }

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConstants.QUEUE_SEARCH_TAG, durable = "true"),
            exchange = @Exchange(value = MqConstants.EXCHANGE_TAG, type = "topic"),
            key = {MqConstants.RK_TAG_MERGED}
    ))
    public void onTagMerged(Map<String, Object> message) {
        try {
            log.info("Received tag.merged event for index update: {}", message);
            // When tags are merged, affected documents need re-indexing
            // This would typically involve querying for all documents with the source tag
            // and updating their tagIds/tagNames in the index
            Object sourceTagIdObj = message.get("sourceTagId");
            Object targetTagIdObj = message.get("targetTagId");
            log.info("Tag merge: source={} -> target={}, documents will be re-indexed by tag service",
                    sourceTagIdObj, targetTagIdObj);
        } catch (Exception e) {
            log.error("Failed to process tag.merged event for index", e);
        }
    }

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConstants.QUEUE_OCR_RESULT, durable = "true"),
            exchange = @Exchange(value = MqConstants.EXCHANGE_OCR, type = "topic"),
            key = {MqConstants.RK_OCR_COMPLETED}
    ))
    public void onOcrCompleted(Map<String, Object> message) {
        try {
            log.info("Received ocr.completed event for index update: {}", message);
            Object documentIdObj = message.get("documentId");
            Object ocrTextObj = message.get("ocrText");

            if (documentIdObj != null && ocrTextObj != null) {
                DocumentIndex doc = new DocumentIndex();
                doc.setId(Long.valueOf(documentIdObj.toString()));
                doc.setOcrText(ocrTextObj.toString());
                doc.setUpdatedAt(LocalDateTime.now());
                searchService.updateDocument(doc);
            }
        } catch (Exception e) {
            log.error("Failed to update index with OCR text", e);
        }
    }

    @SuppressWarnings("unchecked")
    private DocumentIndex buildDocumentIndex(Map<String, Object> message) {
        DocumentIndex doc = new DocumentIndex();

        if (message.get("id") != null) {
            doc.setId(Long.valueOf(message.get("id").toString()));
        }
        if (message.get("documentId") != null) {
            doc.setId(Long.valueOf(message.get("documentId").toString()));
        }
        if (message.get("title") != null) {
            doc.setTitle(message.get("title").toString());
        }
        if (message.get("description") != null) {
            doc.setDescription(message.get("description").toString());
        }
        if (message.get("fileName") != null) {
            doc.setFileName(message.get("fileName").toString());
        }
        if (message.get("fileType") != null) {
            doc.setFileType(message.get("fileType").toString());
        }
        if (message.get("userId") != null) {
            doc.setUserId(Long.valueOf(message.get("userId").toString()));
        }
        if (message.get("tagIds") instanceof List<?> tagIds) {
            doc.setTagIds(tagIds.stream().map(o -> Long.valueOf(o.toString())).toList());
        }
        if (message.get("tagNames") instanceof List<?> tagNames) {
            doc.setTagNames(tagNames.stream().map(Object::toString).toList());
        }
        if (message.get("ocrText") != null) {
            doc.setOcrText(message.get("ocrText").toString());
        }
        if (message.get("status") != null) {
            doc.setStatus(Integer.valueOf(message.get("status").toString()));
        }
        if (message.get("fileSize") != null) {
            doc.setFileSize(Long.valueOf(message.get("fileSize").toString()));
        }
        if (message.get("metadata") instanceof Map<?, ?> metadata) {
            doc.setMetadata((Map<String, Object>) metadata);
        }

        doc.setUpdatedAt(LocalDateTime.now());
        return doc;
    }
}
