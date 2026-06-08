package com.doccase.common.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class TagDTO implements Serializable {

    private Long id;
    private String name;
    private Long parentId;
    private String path;
    private Integer level;
    private Integer sortOrder;
    private String color;
    private String icon;
    private Integer documentCount;
    private LocalDateTime createdAt;
    private List<TagDTO> children;
}
