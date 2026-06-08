package com.doccase.tag.domain.vo;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class BatchDeleteRequest {

    @NotEmpty(message = "标签ID列表不能为空")
    private List<Long> tagIds;
}
