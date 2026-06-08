package com.doccase.search.service;

public interface EmbeddingService {

    float[] generateEmbedding(String text);

    float[][] generateBatchEmbeddings(String[] texts);
}
