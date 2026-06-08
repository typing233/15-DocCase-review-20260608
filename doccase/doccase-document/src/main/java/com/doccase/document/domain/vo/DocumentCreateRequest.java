package com.doccase.document.domain.vo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class DocumentCreateRequest {

    @NotBlank(message = "标题不能为空")
    private String title;
    private String description;
    private List<Long> tagIds;
    private Map<String, Object> metadata;
}
