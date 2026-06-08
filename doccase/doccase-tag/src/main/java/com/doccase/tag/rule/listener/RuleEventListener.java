package com.doccase.tag.rule.listener;

import com.doccase.common.constant.MqConstants;
import com.doccase.common.dto.RuleEvaluateEvent;
import com.doccase.tag.rule.service.RuleEngineService;
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
public class RuleEventListener {

    private final RuleEngineService ruleEngineService;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConstants.QUEUE_RULE_EVALUATE, durable = "true"),
            exchange = @Exchange(value = MqConstants.EXCHANGE_DOCUMENT, type = "topic"),
            key = {MqConstants.RK_DOCUMENT_CREATED, MqConstants.RK_DOCUMENT_UPDATED}
    ))
    @SuppressWarnings("unchecked")
    public void onDocumentEvent(Map<String, Object> message) {
        try {
            String triggerEvent = message.containsKey("action") ?
                    "DOCUMENT_" + message.get("action").toString().toUpperCase() : "DOCUMENT_CREATED";

            RuleEvaluateEvent event = buildEvaluateEvent(message, triggerEvent);
            ruleEngineService.evaluateAndExecute(event);
        } catch (Exception e) {
            log.error("Failed to evaluate rules for document event", e);
        }
    }

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "rule.ocr.queue", durable = "true"),
            exchange = @Exchange(value = MqConstants.EXCHANGE_OCR, type = "topic"),
            key = {MqConstants.RK_OCR_COMPLETED}
    ))
    @SuppressWarnings("unchecked")
    public void onOcrCompleted(Map<String, Object> message) {
        try {
            RuleEvaluateEvent event = buildEvaluateEvent(message, "OCR_COMPLETED");
            ruleEngineService.evaluateAndExecute(event);
        } catch (Exception e) {
            log.error("Failed to evaluate rules for OCR completed event", e);
        }
    }

    @SuppressWarnings("unchecked")
    private RuleEvaluateEvent buildEvaluateEvent(Map<String, Object> message, String triggerEvent) {
        RuleEvaluateEvent event = new RuleEvaluateEvent();
        event.setTriggerEvent(triggerEvent);
        event.setTenantId(getStringOrDefault(message, "tenantId", "default"));

        if (message.get("id") != null) event.setDocumentId(Long.valueOf(message.get("id").toString()));
        if (message.get("documentId") != null) event.setDocumentId(Long.valueOf(message.get("documentId").toString()));
        if (message.get("title") != null) event.setTitle(message.get("title").toString());
        if (message.get("description") != null) event.setDescription(message.get("description").toString());
        if (message.get("fileName") != null) event.setFileName(message.get("fileName").toString());
        if (message.get("fileType") != null) event.setFileType(message.get("fileType").toString());
        if (message.get("mimeType") != null) event.setMimeType(message.get("mimeType").toString());
        if (message.get("fileSize") != null) event.setFileSize(Long.valueOf(message.get("fileSize").toString()));
        if (message.get("userId") != null) event.setUserId(Long.valueOf(message.get("userId").toString()));
        if (message.get("ocrText") != null) event.setOcrText(message.get("ocrText").toString());
        if (message.get("tagIds") instanceof List<?> tagIds) {
            event.setTagIds(tagIds.stream().map(o -> Long.valueOf(o.toString())).toList());
        }
        if (message.get("tagNames") instanceof List<?> tagNames) {
            event.setTagNames(tagNames.stream().map(Object::toString).toList());
        }
        if (message.get("metadata") instanceof Map<?, ?> metadata) {
            event.setMetadata((Map<String, Object>) metadata);
        }

        return event;
    }

    private String getStringOrDefault(Map<String, Object> map, String key, String defaultValue) {
        Object val = map.get(key);
        return val != null ? val.toString() : defaultValue;
    }
}
