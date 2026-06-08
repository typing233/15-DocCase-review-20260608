package com.doccase.tag.domain.vo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

@Data
public class TagMergeRequest implements Serializable {

    @NotNull(message = "Source tag ID is required")
    private Long sourceTagId;

    @NotNull(message = "Target tag ID is required")
    private Long targetTagId;
}
