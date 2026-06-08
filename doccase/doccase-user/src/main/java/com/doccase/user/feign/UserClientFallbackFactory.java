package com.doccase.user.feign;

import com.doccase.common.dto.UserDTO;
import com.doccase.common.response.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Component
public class UserClientFallbackFactory implements FallbackFactory<UserClient> {

    private static final Logger log = LoggerFactory.getLogger(UserClientFallbackFactory.class);

    @Override
    public UserClient create(Throwable cause) {
        log.warn("User service call failed, using fallback: {}", cause.getMessage());
        return id -> ApiResponse.fail("User service unavailable");
    }
}
