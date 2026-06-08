package com.doccase.tag.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("dc_document_tag")
public class DocumentTag implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long documentId;

    private Long tagId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
