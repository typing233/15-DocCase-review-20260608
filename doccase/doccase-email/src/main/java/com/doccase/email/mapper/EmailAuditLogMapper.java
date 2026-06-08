package com.doccase.email.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.doccase.email.domain.entity.EmailAuditLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface EmailAuditLogMapper extends BaseMapper<EmailAuditLog> {
}
