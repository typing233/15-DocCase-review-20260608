package com.doccase.user.controller;

import com.doccase.common.domain.PageResult;
import com.doccase.common.dto.UserDTO;
import com.doccase.common.response.ApiResponse;
import com.doccase.user.domain.vo.UserCreateRequest;
import com.doccase.user.domain.vo.UserUpdateRequest;
import com.doccase.user.domain.vo.UserVO;
import com.doccase.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping
    public ApiResponse<UserVO> createUser(@Valid @RequestBody UserCreateRequest request) {
        return ApiResponse.success(userService.createUser(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<UserVO> updateUser(@PathVariable Long id, @Valid @RequestBody UserUpdateRequest request) {
        return ApiResponse.success(userService.updateUser(id, request));
    }

    @GetMapping("/{id}")
    public ApiResponse<UserVO> getUser(@PathVariable Long id) {
        return ApiResponse.success(userService.getUserById(id));
    }

    @GetMapping("/internal/{id}")
    public ApiResponse<UserDTO> getUserDTO(@PathVariable Long id) {
        return ApiResponse.success(userService.getUserDTOById(id));
    }

    @GetMapping("/internal/by-username")
    public ApiResponse<UserDTO> getUserByUsername(@RequestParam String username) {
        return ApiResponse.success(userService.getUserByUsername(username));
    }

    @GetMapping
    public ApiResponse<PageResult<UserVO>> listUsers(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String keyword) {
        return ApiResponse.success(userService.listUsers(pageNum, pageSize, keyword));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ApiResponse.success();
    }

    @PostMapping("/{id}/roles")
    public ApiResponse<Void> assignRoles(@PathVariable Long id, @RequestBody List<Long> roleIds) {
        userService.assignRoles(id, roleIds);
        return ApiResponse.success();
    }

    @GetMapping("/{id}/roles")
    public ApiResponse<List<String>> getUserRoles(@PathVariable Long id) {
        return ApiResponse.success(userService.getUserRoles(id));
    }
}
