package com.doccase.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.doccase.auth.domain.entity.MfaSecret;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MfaSecretMapper extends BaseMapper<MfaSecret> {
}
