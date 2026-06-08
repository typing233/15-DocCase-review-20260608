package com.doccase.ocr.service;

import com.doccase.common.domain.PageResult;
import com.doccase.ocr.domain.entity.OcrTask;
import com.doccase.ocr.domain.vo.OcrResultVO;
import com.doccase.ocr.domain.vo.OcrTaskCreateRequest;

public interface OcrTaskService {

    OcrTask submitTask(Long userId, OcrTaskCreateRequest request);

    void processTask(Long taskId);

    OcrTask getTaskStatus(Long taskId);

    OcrResultVO getTaskResult(Long taskId);

    PageResult<OcrTask> listTasks(Long userId, int pageNum, int pageSize);
}
