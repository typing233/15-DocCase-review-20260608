package com.doccase.document.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.doccase.document.domain.entity.DocumentVersion;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DocumentVersionMapper extends BaseMapper<DocumentVersion> {
}
