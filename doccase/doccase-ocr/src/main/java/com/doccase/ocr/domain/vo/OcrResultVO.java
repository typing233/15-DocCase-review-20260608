package com.doccase.ocr.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
public class OcrResultVO implements Serializable {

    private Long taskId;

    private Long documentId;

    private String fullText;

    private Double confidence;

    private Integer pageCount;

    private List<Map<String, Object>> pageResults;

    private Long processingTimeMs;

    private String engine;

    private String language;

    private Integer taskStatus;

    private LocalDateTime completedAt;
}
