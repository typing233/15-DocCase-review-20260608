package com.doccase.search.document;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
public class DocumentIndex implements Serializable {

    private Long id;

    private String title;

    private String description;

    private String fileName;

    private String fileType;

    private Long userId;

    private List<Long> tagIds;

    private List<String> tagNames;

    private String ocrText;

    private Map<String, Object> metadata;

    private Integer status;

    private Long fileSize;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}
