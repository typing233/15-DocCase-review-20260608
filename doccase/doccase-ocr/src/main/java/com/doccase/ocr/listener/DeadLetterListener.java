package com.doccase.ocr.listener;

import com.doccase.common.constant.MqConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class DeadLetterListener {

    @RabbitListener(queues = MqConstants.QUEUE_DLX)
    public void onDeadLetter(Map<String, Object> message) {
        log.error("Dead letter received - message could not be processed after all retries: {}", message);

        // Log details for monitoring and alerting
        Object taskId = message.get("taskId");
        Object documentId = message.get("documentId");
        Object error = message.get("error");

        log.error("DLX - taskId: {}, documentId: {}, error: {}", taskId, documentId, error);

        // Here you could:
        // 1. Send alerts to monitoring system
        // 2. Store in a failed-tasks table for manual review
        // 3. Notify administrators
    }
}
