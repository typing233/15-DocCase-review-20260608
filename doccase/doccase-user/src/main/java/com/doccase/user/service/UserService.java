package com.doccase.user.service;

import com.doccase.common.domain.PageResult;
import com.doccase.common.dto.UserDTO;
import com.doccase.user.domain.vo.UserCreateRequest;
import com.doccase.user.domain.vo.UserUpdateRequest;
import com.doccase.user.domain.vo.UserVO;

import java.util.List;

public interface UserService {

    UserVO createUser(UserCreateRequest request);

    UserVO updateUser(Long id, UserUpdateRequest request);

    UserVO getUserById(Long id);

    UserDTO getUserDTOById(Long id);

    UserDTO getUserByUsername(String username);

    PageResult<UserVO> listUsers(int pageNum, int pageSize, String keyword);

    void deleteUser(Long id);

    void assignRoles(Long userId, List<Long> roleIds);

    List<String> getUserRoles(Long userId);
}
