package com.doccase.ocr.feign;

import com.doccase.common.response.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Component
public class DocumentClientFallbackFactory implements FallbackFactory<DocumentClient> {

    private static final Logger log = LoggerFactory.getLogger(DocumentClientFallbackFactory.class);

    @Override
    public DocumentClient create(Throwable cause) {
        log.error("Document service call failed: {}", cause.getMessage());
        return id -> ApiResponse.fail("Document service unavailable");
    }
}
