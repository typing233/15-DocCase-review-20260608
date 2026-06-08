package com.doccase.document.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.doccase.document.domain.entity.Document;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DocumentMapper extends BaseMapper<Document> {
}
