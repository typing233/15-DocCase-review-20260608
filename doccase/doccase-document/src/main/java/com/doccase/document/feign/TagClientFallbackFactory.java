package com.doccase.document.feign;

import com.doccase.common.dto.TagDTO;
import com.doccase.common.response.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class TagClientFallbackFactory implements FallbackFactory<TagClient> {

    private static final Logger log = LoggerFactory.getLogger(TagClientFallbackFactory.class);

    @Override
    public TagClient create(Throwable cause) {
        log.warn("Tag service call failed, using fallback: {}", cause.getMessage());
        return ids -> ApiResponse.success(Collections.emptyList());
    }
}
