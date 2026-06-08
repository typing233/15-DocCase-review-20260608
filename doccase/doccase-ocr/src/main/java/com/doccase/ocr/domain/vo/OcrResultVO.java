package com.doccase.ocr.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
public class OcrResultVO implements Serializable {

    private Long taskId;

    private Long documentId;

    private String fullText;

    private BigDecimal confidence;

    private List<Map<String, Object>> pageResults;

    private Map<String, Object> structuredData;

    private Integer processingTimeMs;

    private String engineUsed;

    private Integer taskStatus;

    private LocalDateTime completedAt;
}
