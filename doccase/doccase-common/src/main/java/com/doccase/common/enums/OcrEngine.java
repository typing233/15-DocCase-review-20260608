package com.doccase.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum OcrEngine {

    TESSERACT("tesseract", "Tesseract OCR"),
    PADDLE_OCR("paddleocr", "PaddleOCR"),
    AUTO("auto", "自动选择");

    private final String code;
    private final String desc;
}
