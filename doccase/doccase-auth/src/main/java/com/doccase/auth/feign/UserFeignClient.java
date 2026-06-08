package com.doccase.auth.feign;

import com.doccase.common.dto.UserDTO;
import com.doccase.common.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@FeignClient(name = "doccase-user", path = "/users")
public interface UserFeignClient {

    @GetMapping("/internal/by-username")
    ApiResponse<UserDTO> getUserByUsername(@RequestParam("username") String username);

    @GetMapping("/internal/{id}")
    ApiResponse<UserDTO> getUserDTO(@PathVariable("id") Long id);

    @PostMapping
    ApiResponse<Object> createUser(@RequestBody Map<String, Object> request);
}
