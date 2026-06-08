package com.doccase.ocr.engine;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OcrEngineDispatcher {

    private final TesseractEngine tesseractEngine;
    private final PaddleOcrEngine paddleOcrEngine;

    /**
     * Selects the appropriate OCR engine based on language.
     * chi_sim -> PaddleOCR (better for Chinese)
     * eng -> Tesseract (better for English/Latin)
     * auto -> detect based on content (defaults to PaddleOCR for broader support)
     */
    public OcrEngine dispatch(String language) {
        if (language == null || language.isBlank() || "auto".equalsIgnoreCase(language)) {
            log.info("Auto-detecting OCR engine, defaulting to PaddleOCR");
            return paddleOcrEngine;
        }

        return switch (language.toLowerCase()) {
            case "chi_sim", "chi_tra", "ch", "chinese" -> {
                log.info("Dispatching to PaddleOCR for language: {}", language);
                yield paddleOcrEngine;
            }
            case "eng", "english", "fra", "deu", "spa", "ita", "por" -> {
                log.info("Dispatching to Tesseract for language: {}", language);
                yield tesseractEngine;
            }
            default -> {
                log.info("Unknown language '{}', defaulting to Tesseract", language);
                yield tesseractEngine;
            }
        };
    }
}
