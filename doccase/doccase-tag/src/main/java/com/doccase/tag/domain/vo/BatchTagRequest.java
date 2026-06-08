package com.doccase.tag.domain.vo;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class BatchTagRequest {

    @NotEmpty(message = "文档ID列表不能为空")
    private List<Long> documentIds;

    @NotEmpty(message = "标签ID列表不能为空")
    private List<Long> tagIds;

    @NotNull(message = "操作类型不能为空")
    private Action action;

    public enum Action {
        ADD, REMOVE
    }
}
