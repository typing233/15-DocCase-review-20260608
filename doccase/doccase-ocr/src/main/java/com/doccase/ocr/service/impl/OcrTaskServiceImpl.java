package com.doccase.ocr.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.doccase.common.constant.MqConstants;
import com.doccase.common.domain.PageResult;
import com.doccase.common.exception.BizException;
import com.doccase.ocr.domain.entity.OcrResult;
import com.doccase.ocr.domain.entity.OcrTask;
import com.doccase.ocr.domain.vo.OcrResultVO;
import com.doccase.ocr.domain.vo.OcrTaskCreateRequest;
import com.doccase.ocr.engine.OcrEngine;
import com.doccase.ocr.engine.OcrEngineDispatcher;
import com.doccase.ocr.engine.OcrEngineResult;
import com.doccase.ocr.mapper.OcrResultMapper;
import com.doccase.ocr.mapper.OcrTaskMapper;
import com.doccase.ocr.pipeline.PreprocessPipeline;
import com.doccase.ocr.service.OcrTaskService;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class OcrTaskServiceImpl implements OcrTaskService {

    private final OcrTaskMapper ocrTaskMapper;
    private final OcrResultMapper ocrResultMapper;
    private final OcrEngineDispatcher engineDispatcher;
    private final PreprocessPipeline preprocessPipeline;
    private final RabbitTemplate rabbitTemplate;
    private final MinioClient minioClient;

    @Value("${doccase.storage.minio.bucket:doccase}")
    private String bucket;

    private static final int MAX_RETRIES = 3;

    @Override
    @Transactional
    public OcrTask submitTask(Long userId, OcrTaskCreateRequest request) {
        OcrTask task = new OcrTask();
        task.setDocumentId(request.getDocumentId());
        task.setEngine(request.getEngine() != null ? request.getEngine() : "auto");
        task.setLanguage(request.getLanguage() != null ? request.getLanguage() : "auto");
        task.setStatus(0); // pending
        task.setRetryCount(0);
        task.setMaxRetries(MAX_RETRIES);
        task.setCreatedBy(userId);
        task.setCreatedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());

        ocrTaskMapper.insert(task);

        // Publish task to MQ for async processing
        Map<String, Object> message = new HashMap<>();
        message.put("taskId", task.getId());
        message.put("documentId", request.getDocumentId());
        message.put("engine", task.getEngine());
        message.put("language", task.getLanguage());
        rabbitTemplate.convertAndSend(MqConstants.EXCHANGE_OCR, MqConstants.RK_OCR_SUBMIT, message);

        log.info("OCR task submitted: taskId={}, documentId={}", task.getId(), request.getDocumentId());
        return task;
    }

    @Override
    @Transactional
    public void processTask(Long taskId) {
        OcrTask task = ocrTaskMapper.selectById(taskId);
        if (task == null) {
            log.error("OCR task not found: {}", taskId);
            return;
        }

        // Update status to processing
        task.setStatus(1); // processing
        task.setUpdatedAt(LocalDateTime.now());
        ocrTaskMapper.updateById(task);

        try {
            // Fetch document file from storage
            byte[] imageData = fetchDocumentFile(task.getDocumentId());

            // Run preprocessing pipeline
            byte[] preprocessed = preprocessPipeline.execute(imageData);

            // Select and run OCR engine
            OcrEngine engine = engineDispatcher.dispatch(task.getLanguage());
            OcrEngineResult result = engine.recognize(preprocessed, task.getLanguage());

            // Save result
            OcrResult ocrResult = new OcrResult();
            ocrResult.setTaskId(taskId);
            ocrResult.setDocumentId(task.getDocumentId());
            ocrResult.setFullText(result.getText());
            ocrResult.setConfidence(result.getConfidence());
            ocrResult.setPageCount(result.getPageResults() != null ? result.getPageResults().size() : 1);
            ocrResult.setPageResults(result.getPageResults());
            ocrResult.setProcessingTimeMs(result.getProcessingTimeMs());
            ocrResult.setEngine(engine.getEngineName());
            ocrResult.setLanguage(task.getLanguage());
            ocrResult.setCreatedAt(LocalDateTime.now());
            ocrResultMapper.insert(ocrResult);

            // Update task status
            task.setStatus(2); // completed
            task.setProcessingTimeMs(result.getProcessingTimeMs());
            task.setCompletedAt(LocalDateTime.now());
            task.setUpdatedAt(LocalDateTime.now());
            ocrTaskMapper.updateById(task);

            // Publish completion event
            Map<String, Object> event = new HashMap<>();
            event.put("taskId", taskId);
            event.put("documentId", task.getDocumentId());
            event.put("ocrText", result.getText());
            event.put("confidence", result.getConfidence());
            event.put("engine", engine.getEngineName());
            rabbitTemplate.convertAndSend(MqConstants.EXCHANGE_OCR, MqConstants.RK_OCR_COMPLETED, event);

            log.info("OCR task completed: taskId={}, processingTime={}ms",
                    taskId, result.getProcessingTimeMs());

        } catch (Exception e) {
            log.error("OCR task failed: taskId={}", taskId, e);
            handleTaskFailure(task, e);
        }
    }

    @Override
    public OcrTask getTaskStatus(Long taskId) {
        OcrTask task = ocrTaskMapper.selectById(taskId);
        if (task == null) {
            throw new BizException("OCR task not found");
        }
        return task;
    }

    @Override
    public OcrResultVO getTaskResult(Long taskId) {
        OcrTask task = ocrTaskMapper.selectById(taskId);
        if (task == null) {
            throw new BizException("OCR task not found");
        }

        LambdaQueryWrapper<OcrResult> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(OcrResult::getTaskId, taskId);
        OcrResult result = ocrResultMapper.selectOne(queryWrapper);

        if (result == null) {
            throw new BizException("OCR result not available yet");
        }

        OcrResultVO vo = new OcrResultVO();
        vo.setTaskId(taskId);
        vo.setDocumentId(result.getDocumentId());
        vo.setFullText(result.getFullText());
        vo.setConfidence(result.getConfidence());
        vo.setPageCount(result.getPageCount());
        vo.setPageResults(result.getPageResults());
        vo.setProcessingTimeMs(result.getProcessingTimeMs());
        vo.setEngine(result.getEngine());
        vo.setLanguage(result.getLanguage());
        vo.setTaskStatus(task.getStatus());
        vo.setCompletedAt(task.getCompletedAt());

        return vo;
    }

    @Override
    public PageResult<OcrTask> listTasks(Long userId, int pageNum, int pageSize) {
        LambdaQueryWrapper<OcrTask> queryWrapper = new LambdaQueryWrapper<>();
        if (userId != null) {
            queryWrapper.eq(OcrTask::getCreatedBy, userId);
        }
        queryWrapper.orderByDesc(OcrTask::getCreatedAt);

        Page<OcrTask> page = new Page<>(pageNum, pageSize);
        Page<OcrTask> result = ocrTaskMapper.selectPage(page, queryWrapper);

        return PageResult.of(result.getRecords(), result.getTotal(), pageNum, pageSize);
    }

    private byte[] fetchDocumentFile(Long documentId) {
        try {
            // Construct storage path based on document ID
            String storagePath = "documents/" + documentId;
            InputStream stream = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucket)
                            .object(storagePath)
                            .build()
            );
            return stream.readAllBytes();
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch document file: " + documentId, e);
        }
    }

    private void handleTaskFailure(OcrTask task, Exception e) {
        task.setRetryCount(task.getRetryCount() + 1);
        task.setErrorMessage(e.getMessage());
        task.setUpdatedAt(LocalDateTime.now());

        if (task.getRetryCount() >= task.getMaxRetries()) {
            // Max retries exceeded, mark as failed
            task.setStatus(3); // failed
            ocrTaskMapper.updateById(task);

            // Publish failure event
            Map<String, Object> event = new HashMap<>();
            event.put("taskId", task.getId());
            event.put("documentId", task.getDocumentId());
            event.put("error", e.getMessage());
            event.put("retryCount", task.getRetryCount());
            rabbitTemplate.convertAndSend(MqConstants.EXCHANGE_OCR, MqConstants.RK_OCR_FAILED, event);

            log.error("OCR task permanently failed after {} retries: taskId={}",
                    task.getRetryCount(), task.getId());
        } else {
            // Retry with exponential backoff
            task.setStatus(0); // back to pending
            ocrTaskMapper.updateById(task);

            // Re-submit with delay (exponential backoff: 2^retryCount seconds)
            long delayMs = (long) Math.pow(2, task.getRetryCount()) * 1000;
            Map<String, Object> message = new HashMap<>();
            message.put("taskId", task.getId());
            message.put("documentId", task.getDocumentId());
            message.put("engine", task.getEngine());
            message.put("language", task.getLanguage());
            message.put("retryCount", task.getRetryCount());

            rabbitTemplate.convertAndSend(MqConstants.EXCHANGE_OCR, MqConstants.RK_OCR_SUBMIT, message,
                    msg -> {
                        msg.getMessageProperties().setDelay((int) delayMs);
                        return msg;
                    });

            log.warn("OCR task retry scheduled: taskId={}, retry={}, delay={}ms",
                    task.getId(), task.getRetryCount(), delayMs);
        }
    }
}
