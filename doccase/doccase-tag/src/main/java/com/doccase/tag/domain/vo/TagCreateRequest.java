package com.doccase.tag.domain.vo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

@Data
public class TagCreateRequest implements Serializable {

    @NotBlank(message = "Tag name is required")
    private String name;

    private Long parentId;

    private String color;

    private String icon;

    private Integer sortOrder;
}
