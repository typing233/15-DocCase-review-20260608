package com.doccase.user.feign;

import com.doccase.common.dto.UserDTO;
import com.doccase.common.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "doccase-user", fallbackFactory = UserClientFallbackFactory.class)
public interface UserClient {

    @GetMapping("/users/{id}")
    ApiResponse<UserDTO> getUserById(@PathVariable("id") Long id);
}
