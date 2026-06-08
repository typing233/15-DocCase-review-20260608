package com.doccase.document.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName(value = "dc_chunk_upload", autoResultMap = true)
public class ChunkUploadRecord {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String uploadId;
    private Long userId;
    private String fileName;
    private String fileHash;
    private Long totalSize;
    private Integer chunkSize;
    private Integer totalChunks;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<Integer> uploadedChunks;

    private Integer status;
    private LocalDateTime expireAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
