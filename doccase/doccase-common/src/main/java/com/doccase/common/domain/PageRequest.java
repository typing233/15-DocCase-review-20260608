package com.doccase.common.domain;

import lombok.Data;

import java.io.Serializable;

@Data
public class PageRequest implements Serializable {

    private Integer pageNum = 1;
    private Integer pageSize = 20;
    private String orderBy;
    private Boolean asc = true;

    public int getOffset() {
        return (pageNum - 1) * pageSize;
    }
}
