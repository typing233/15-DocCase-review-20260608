package com.doccase.ocr.listener;

import com.doccase.common.constant.MqConstants;
import com.doccase.ocr.service.OcrTaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class OcrTaskListener {

    private final OcrTaskService ocrTaskService;

    @RabbitListener(queues = MqConstants.QUEUE_OCR_TASK)
    public void onOcrSubmit(Map<String, Object> message) {
        try {
            log.info("Received OCR task message: {}", message);
            Object taskIdObj = message.get("taskId");
            if (taskIdObj != null) {
                Long taskId = Long.valueOf(taskIdObj.toString());
                ocrTaskService.processTask(taskId);
            } else {
                log.error("OCR task message missing taskId: {}", message);
            }
        } catch (Exception e) {
            log.error("Error processing OCR task message: {}", message, e);
            throw e; // Let RabbitMQ handle the retry/DLX
        }
    }
}
