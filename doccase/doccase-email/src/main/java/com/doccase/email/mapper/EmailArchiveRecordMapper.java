package com.doccase.email.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.doccase.email.domain.entity.EmailArchiveRecord;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface EmailArchiveRecordMapper extends BaseMapper<EmailArchiveRecord> {
}
