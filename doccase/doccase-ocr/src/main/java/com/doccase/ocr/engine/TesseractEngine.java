package com.doccase.ocr.engine;

import com.doccase.ocr.config.OcrEngineConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class TesseractEngine implements OcrEngine {

    private final OcrEngineConfig config;

    @Override
    public OcrEngineResult recognize(byte[] image, String language) {
        long startTime = System.currentTimeMillis();
        Path tempInput = null;
        Path tempOutput = null;

        try {
            // Write image to temp file
            tempInput = Files.createTempFile("ocr_input_", ".png");
            Files.write(tempInput, image);

            tempOutput = Files.createTempFile("ocr_output_", "");
            String outputBase = tempOutput.toString();

            // Build tesseract command
            List<String> command = new ArrayList<>();
            command.add(config.getTesseract().getCommand());
            command.add(tempInput.toString());
            command.add(outputBase);
            command.add("-l");
            command.add(language != null ? language : "eng");
            command.add("--dpi");
            command.add("300");
            command.add("-c");
            command.add("tessedit_create_tsv=1");

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.environment().put("TESSDATA_PREFIX", config.getTesseract().getDataPath());
            pb.redirectErrorStream(true);

            Process process = pb.start();
            boolean finished = process.waitFor(config.getTesseract().getTimeout(), TimeUnit.SECONDS);

            if (!finished) {
                process.destroyForcibly();
                throw new RuntimeException("Tesseract process timed out");
            }

            if (process.exitValue() != 0) {
                String error = new String(process.getInputStream().readAllBytes());
                throw new RuntimeException("Tesseract failed with exit code " + process.exitValue() + ": " + error);
            }

            // Read output text
            Path textFile = Path.of(outputBase + ".txt");
            String text = "";
            if (Files.exists(textFile)) {
                text = Files.readString(textFile);
                Files.deleteIfExists(textFile);
            }

            // Parse TSV for confidence (if available)
            double confidence = parseConfidence(Path.of(outputBase + ".tsv"));

            long processingTime = System.currentTimeMillis() - startTime;

            Map<String, Object> pageResult = new HashMap<>();
            pageResult.put("page", 1);
            pageResult.put("text", text);
            pageResult.put("confidence", confidence);

            return new OcrEngineResult(text, confidence, List.of(pageResult), processingTime);

        } catch (Exception e) {
            log.error("Tesseract OCR failed", e);
            long processingTime = System.currentTimeMillis() - startTime;
            throw new RuntimeException("Tesseract OCR processing failed: " + e.getMessage(), e);
        } finally {
            try {
                if (tempInput != null) Files.deleteIfExists(tempInput);
                if (tempOutput != null) Files.deleteIfExists(tempOutput);
            } catch (IOException ignored) {
            }
        }
    }

    @Override
    public String getEngineName() {
        return "tesseract";
    }

    private double parseConfidence(Path tsvFile) {
        try {
            if (!Files.exists(tsvFile)) {
                return 0.0;
            }
            List<String> lines = Files.readAllLines(tsvFile);
            double totalConf = 0;
            int count = 0;
            for (int i = 1; i < lines.size(); i++) {
                String[] parts = lines.get(i).split("\t");
                if (parts.length >= 11) {
                    try {
                        double conf = Double.parseDouble(parts[10]);
                        if (conf >= 0) {
                            totalConf += conf;
                            count++;
                        }
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
            Files.deleteIfExists(tsvFile);
            return count > 0 ? totalConf / count : 0.0;
        } catch (IOException e) {
            return 0.0;
        }
    }
}
