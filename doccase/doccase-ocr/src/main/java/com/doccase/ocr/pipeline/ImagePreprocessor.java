package com.doccase.ocr.pipeline;

public interface ImagePreprocessor {

    byte[] process(byte[] image);

    int getOrder();
}
