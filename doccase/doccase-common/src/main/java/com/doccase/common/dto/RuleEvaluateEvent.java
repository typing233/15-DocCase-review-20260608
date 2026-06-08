package com.doccase.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RuleEvaluateEvent implements Serializable {

    private Long documentId;
    private String tenantId;
    private String triggerEvent;
    private String title;
    private String description;
    private String fileName;
    private String fileType;
    private String mimeType;
    private Long fileSize;
    private Long userId;
    private String ocrText;
    private List<Long> tagIds;
    private List<String> tagNames;
    private Map<String, Object> metadata;
}
