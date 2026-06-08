package com.doccase.document.feign;

import com.doccase.common.dto.TagDTO;
import com.doccase.common.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Collections;
import java.util.List;

@FeignClient(name = "doccase-tag", fallbackFactory = TagClientFallbackFactory.class)
public interface TagClient {

    @GetMapping("/tags/batch")
    ApiResponse<List<TagDTO>> getTagsByIds(@RequestParam("ids") List<Long> ids);
}
