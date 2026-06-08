package com.doccase.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.doccase.user.domain.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    @Select("SELECT r.code FROM dc_user_role ur JOIN dc_role r ON ur.role_id = r.id WHERE ur.user_id = #{userId}")
    List<String> selectRoleCodesByUserId(@Param("userId") Long userId);
}
