package com.doccase.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.doccase.auth.domain.entity.AbacPolicy;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AbacPolicyMapper extends BaseMapper<AbacPolicy> {
}
