package com.doccase.ocr.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "doccase.ocr")
public class OcrEngineConfig {

    private Tesseract tesseract = new Tesseract();
    private PaddleOcr paddleOcr = new PaddleOcr();

    @Data
    public static class Tesseract {
        private String dataPath = "/usr/share/tesseract-ocr/4.00/tessdata";
        private String command = "tesseract";
        private int timeout = 120;
    }

    @Data
    public static class PaddleOcr {
        private String endpoint = "http://localhost:8866";
        private String command = "paddleocr";
        private boolean useHttp = true;
        private int timeout = 180;
    }
}
