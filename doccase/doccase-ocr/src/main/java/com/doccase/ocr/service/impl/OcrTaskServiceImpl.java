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
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
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
        task.setUserId(userId);
        task.setEngine(request.getEngine() != null ? request.getEngine() : "auto");
        task.setLanguage(request.getLanguage() != null ? request.getLanguage() : "chi_sim");
        task.setSourcePath("documents/" + request.getDocumentId());
        task.setFileType("image");
        task.setStatus(0);
        task.setRetryCount(0);
        task.setMaxRetries(MAX_RETRIES);
        task.setCreatedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());

        ocrTaskMapper.insert(task);

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

        task.setStatus(1); // preprocessing
        task.setStartedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());
        ocrTaskMapper.updateById(task);

        try {
            byte[] imageData = fetchDocumentFile(task.getSourcePath());

            byte[] preprocessed = preprocessPipeline.execute(imageData);

            task.setStatus(2); // recognizing
            task.setUpdatedAt(LocalDateTime.now());
            ocrTaskMapper.updateById(task);

            OcrEngine engine = engineDispatcher.dispatch(task.getLanguage());
            OcrEngineResult result = engine.recognize(preprocessed, task.getLanguage());

            OcrResult ocrResult = new OcrResult();
            ocrResult.setTaskId(taskId);
            ocrResult.setDocumentId(task.getDocumentId());
            ocrResult.setEngineUsed(engine.getEngineName());
            ocrResult.setFullText(result.getText());
            ocrResult.setConfidence(result.getConfidence() != null
                    ? BigDecimal.valueOf(result.getConfidence()).setScale(4, RoundingMode.HALF_UP)
                    : null);
            ocrResult.setPageResults(result.getPageResults());
            ocrResult.setProcessingTimeMs(result.getProcessingTimeMs() != null
                    ? result.getProcessingTimeMs().intValue() : null);
            ocrResult.setCreatedAt(LocalDateTime.now());
            ocrResultMapper.insert(ocrResult);

            task.setStatus(3); // completed
            task.setCompletedAt(LocalDateTime.now());
            task.setUpdatedAt(LocalDateTime.now());
            ocrTaskMapper.updateById(task);

            Map<String, Object> event = new HashMap<>();
            event.put("taskId", taskId);
            event.put("documentId", task.getDocumentId());
            event.put("ocrText", result.getText());
            event.put("confidence", result.getConfidence());
            event.put("engine", engine.getEngineName());
            rabbitTemplate.convertAndSend(MqConstants.EXCHANGE_OCR, MqConstants.RK_OCR_COMPLETED, event);

            log.info("OCR task completed: taskId={}, engine={}", taskId, engine.getEngineName());

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
        vo.setPageResults(result.getPageResults());
        vo.setStructuredData(result.getStructuredData());
        vo.setProcessingTimeMs(result.getProcessingTimeMs());
        vo.setEngineUsed(result.getEngineUsed());
        vo.setTaskStatus(task.getStatus());
        vo.setCompletedAt(task.getCompletedAt());

        return vo;
    }

    @Override
    public PageResult<OcrTask> listTasks(Long userId, int pageNum, int pageSize) {
        LambdaQueryWrapper<OcrTask> queryWrapper = new LambdaQueryWrapper<>();
        if (userId != null) {
            queryWrapper.eq(OcrTask::getUserId, userId);
        }
        queryWrapper.orderByDesc(OcrTask::getCreatedAt);

        Page<OcrTask> page = new Page<>(pageNum, pageSize);
        Page<OcrTask> resultPage = ocrTaskMapper.selectPage(page, queryWrapper);

        return PageResult.of(resultPage.getRecords(), resultPage.getTotal(), pageNum, pageSize);
    }

    private byte[] fetchDocumentFile(String storagePath) {
        try {
            InputStream stream = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucket)
                            .object(storagePath)
                            .build()
            );
            return stream.readAllBytes();
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch document file: " + storagePath, e);
        }
    }

    private void handleTaskFailure(OcrTask task, Exception e) {
        task.setRetryCount(task.getRetryCount() + 1);
        String errMsg = e.getMessage() != null ? e.getMessage() : "Unknown error";
        task.setErrorMessage(errMsg.length() > 1000 ? errMsg.substring(0, 1000) : errMsg);
        task.setUpdatedAt(LocalDateTime.now());

        if (task.getRetryCount() >= task.getMaxRetries()) {
            task.setStatus(4); // failed
            ocrTaskMapper.updateById(task);

            Map<String, Object> event = new HashMap<>();
            event.put("taskId", task.getId());
            event.put("documentId", task.getDocumentId());
            event.put("error", task.getErrorMessage());
            event.put("retryCount", task.getRetryCount());
            rabbitTemplate.convertAndSend(MqConstants.EXCHANGE_OCR, MqConstants.RK_OCR_FAILED, event);

            log.error("OCR task permanently failed after {} retries: taskId={}", task.getRetryCount(), task.getId());
        } else {
            task.setStatus(0); // back to pending for retry
            ocrTaskMapper.updateById(task);

            long delayMs = (long) Math.pow(2, task.getRetryCount()) * 2000;
            Map<String, Object> message = new HashMap<>();
            message.put("taskId", task.getId());
            message.put("documentId", task.getDocumentId());
            message.put("engine", task.getEngine());
            message.put("language", task.getLanguage());

            rabbitTemplate.convertAndSend(MqConstants.EXCHANGE_OCR, MqConstants.RK_OCR_SUBMIT, message,
                    msg -> {
                        msg.getMessageProperties().setDelay((int) delayMs);
                        return msg;
                    });

            log.warn("OCR task retry scheduled: taskId={}, retry={}, delay={}ms", task.getId(), task.getRetryCount(), delayMs);
        }
    }
}
