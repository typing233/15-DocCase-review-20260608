package com.doccase.tag.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class TagTreeVO implements Serializable {

    private Long id;

    private String name;

    private Long parentId;

    private String path;

    private Integer level;

    private String color;

    private String icon;

    private Integer documentCount;

    private List<TagTreeVO> children;
}
