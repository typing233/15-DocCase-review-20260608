package com.doccase.ocr.pipeline;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PreprocessPipeline {

    private final List<ImagePreprocessor> preprocessors;

    /**
     * Chains all image preprocessors in order (by getOrder()) and applies them sequentially.
     */
    public byte[] execute(byte[] image) {
        if (image == null || image.length == 0) {
            throw new IllegalArgumentException("Image data cannot be null or empty");
        }

        log.info("Starting preprocessing pipeline with {} processors", preprocessors.size());

        List<ImagePreprocessor> sorted = preprocessors.stream()
                .sorted(Comparator.comparingInt(ImagePreprocessor::getOrder))
                .toList();

        byte[] result = image;
        for (ImagePreprocessor processor : sorted) {
            log.debug("Executing processor: {} (order={})",
                    processor.getClass().getSimpleName(), processor.getOrder());
            result = processor.process(result);
        }

        log.info("Preprocessing pipeline completed, output size: {} bytes", result.length);
        return result;
    }
}
