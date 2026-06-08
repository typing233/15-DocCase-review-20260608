package com.doccase.ocr.engine;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OcrEngineResult {

    private String text;

    private double confidence;

    private List<Map<String, Object>> pageResults;

    private long processingTimeMs;
}
