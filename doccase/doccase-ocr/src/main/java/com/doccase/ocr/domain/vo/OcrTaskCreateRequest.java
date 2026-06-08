package com.doccase.ocr.domain.vo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

@Data
public class OcrTaskCreateRequest implements Serializable {

    @NotNull(message = "Document ID is required")
    private Long documentId;

    private String engine;

    private String language;
}
