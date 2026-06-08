package com.doccase.ocr.engine;

import com.doccase.ocr.config.OcrEngineConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaddleOcrEngine implements OcrEngine {

    private final OcrEngineConfig config;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public OcrEngineResult recognize(byte[] image, String language) {
        long startTime = System.currentTimeMillis();

        if (config.getPaddleOcr().isUseHttp()) {
            return recognizeViaHttp(image, language, startTime);
        } else {
            return recognizeViaCli(image, language, startTime);
        }
    }

    @Override
    public String getEngineName() {
        return "paddleocr";
    }

    private OcrEngineResult recognizeViaHttp(byte[] image, String language, long startTime) {
        try {
            String endpoint = config.getPaddleOcr().getEndpoint() + "/predict/ocr_system";
            URL url = new URL(endpoint);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/octet-stream");
            conn.setDoOutput(true);
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(config.getPaddleOcr().getTimeout() * 1000);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(image);
            }

            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                throw new RuntimeException("PaddleOCR HTTP API returned status " + responseCode);
            }

            String responseBody;
            try (InputStream is = conn.getInputStream()) {
                responseBody = new String(is.readAllBytes());
            }

            // Parse PaddleOCR response
            JsonNode root = objectMapper.readTree(responseBody);
            StringBuilder textBuilder = new StringBuilder();
            double totalConfidence = 0;
            int blockCount = 0;

            List<Map<String, Object>> pageResults = new ArrayList<>();
            Map<String, Object> pageResult = new HashMap<>();

            if (root.has("results")) {
                JsonNode results = root.get("results");
                for (JsonNode block : results) {
                    String text = block.has("text") ? block.get("text").asText() : "";
                    double conf = block.has("confidence") ? block.get("confidence").asDouble() : 0;
                    textBuilder.append(text).append("\n");
                    totalConfidence += conf;
                    blockCount++;
                }
            }

            String fullText = textBuilder.toString().trim();
            double avgConfidence = blockCount > 0 ? totalConfidence / blockCount : 0;

            pageResult.put("page", 1);
            pageResult.put("text", fullText);
            pageResult.put("confidence", avgConfidence);
            pageResults.add(pageResult);

            long processingTime = System.currentTimeMillis() - startTime;
            return new OcrEngineResult(fullText, avgConfidence, pageResults, processingTime);

        } catch (Exception e) {
            log.error("PaddleOCR HTTP recognition failed", e);
            throw new RuntimeException("PaddleOCR HTTP processing failed: " + e.getMessage(), e);
        }
    }

    private OcrEngineResult recognizeViaCli(byte[] image, String language, long startTime) {
        Path tempInput = null;
        try {
            tempInput = Files.createTempFile("paddle_input_", ".png");
            Files.write(tempInput, image);

            List<String> command = new ArrayList<>();
            command.add(config.getPaddleOcr().getCommand());
            command.add("--image_dir");
            command.add(tempInput.toString());
            command.add("--use_angle_cls");
            command.add("true");
            if ("chi_sim".equals(language) || "ch".equals(language)) {
                command.add("--lang");
                command.add("ch");
            } else {
                command.add("--lang");
                command.add("en");
            }

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);

            Process process = pb.start();
            String output = new String(process.getInputStream().readAllBytes());
            boolean finished = process.waitFor(config.getPaddleOcr().getTimeout(), TimeUnit.SECONDS);

            if (!finished) {
                process.destroyForcibly();
                throw new RuntimeException("PaddleOCR process timed out");
            }

            // Parse CLI output - extract text lines
            StringBuilder textBuilder = new StringBuilder();
            String[] lines = output.split("\n");
            for (String line : lines) {
                if (line.contains("rec text:")) {
                    String text = line.substring(line.indexOf("rec text:") + 9).trim();
                    textBuilder.append(text).append("\n");
                }
            }

            String fullText = textBuilder.toString().trim();
            long processingTime = System.currentTimeMillis() - startTime;

            Map<String, Object> pageResult = new HashMap<>();
            pageResult.put("page", 1);
            pageResult.put("text", fullText);
            pageResult.put("confidence", 0.85);

            return new OcrEngineResult(fullText, 0.85, List.of(pageResult), processingTime);

        } catch (Exception e) {
            log.error("PaddleOCR CLI recognition failed", e);
            throw new RuntimeException("PaddleOCR CLI processing failed: " + e.getMessage(), e);
        } finally {
            try {
                if (tempInput != null) Files.deleteIfExists(tempInput);
            } catch (IOException ignored) {
            }
        }
    }
}
