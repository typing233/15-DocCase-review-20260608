package com.doccase.document.domain.vo;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ChunkUploadRequest {

    @NotBlank(message = "文件名不能为空")
    private String fileName;

    @NotNull(message = "文件大小不能为空")
    @Min(value = 1, message = "文件大小必须大于0")
    private Long totalSize;

    @NotNull(message = "分片大小不能为空")
    private Integer chunkSize;

    private String fileHash;
}
