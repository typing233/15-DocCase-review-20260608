package com.doccase.email.feign;

import com.doccase.common.dto.DocumentDTO;
import com.doccase.common.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

@FeignClient(name = "doccase-document", path = "/documents",
        configuration = FeignMultipartConfig.class)
public interface DocumentServiceClient {

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ApiResponse<DocumentDTO> createDocument(
            @RequestHeader("X-User-Id") Long userId,
            @RequestPart("file") MultipartFile file,
            @RequestPart("data") DocumentCreateDTO data
    );
}
