package com.doccase.ocr.engine;

public interface OcrEngine {

    OcrEngineResult recognize(byte[] image, String language);

    String getEngineName();
}
