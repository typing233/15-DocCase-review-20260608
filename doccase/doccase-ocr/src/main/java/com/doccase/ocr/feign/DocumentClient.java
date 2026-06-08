package com.doccase.ocr.feign;

import com.doccase.common.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "doccase-document", fallbackFactory = DocumentClientFallbackFactory.class)
public interface DocumentClient {

    @GetMapping("/documents/{id}/storage-path")
    ApiResponse<String> getDocumentStoragePath(@PathVariable("id") Long id);
}
