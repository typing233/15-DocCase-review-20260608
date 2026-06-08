package com.doccase.ocr.controller;

import com.doccase.common.domain.PageResult;
import com.doccase.common.response.ApiResponse;
import com.doccase.ocr.domain.entity.OcrTask;
import com.doccase.ocr.domain.vo.OcrResultVO;
import com.doccase.ocr.domain.vo.OcrTaskCreateRequest;
import com.doccase.ocr.service.OcrTaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/ocr")
@RequiredArgsConstructor
public class OcrTaskController {

    private final OcrTaskService ocrTaskService;

    @PostMapping("/tasks")
    public ApiResponse<OcrTask> submitTask(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody OcrTaskCreateRequest request) {
        return ApiResponse.success(ocrTaskService.submitTask(userId, request));
    }

    @GetMapping("/tasks/{taskId}/status")
    public ApiResponse<OcrTask> getTaskStatus(@PathVariable Long taskId) {
        return ApiResponse.success(ocrTaskService.getTaskStatus(taskId));
    }

    @GetMapping("/tasks/{taskId}/result")
    public ApiResponse<OcrResultVO> getTaskResult(@PathVariable Long taskId) {
        return ApiResponse.success(ocrTaskService.getTaskResult(taskId));
    }

    @GetMapping("/tasks")
    public ApiResponse<PageResult<OcrTask>> listTasks(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.success(ocrTaskService.listTasks(userId, pageNum, pageSize));
    }
}
