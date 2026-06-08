package com.doccase.document.domain.vo;

import lombok.Data;

import java.util.List;

@Data
public class DocumentQueryRequest {

    private String keyword;
    private List<Long> tagIds;
    private String fileType;
    private Integer status;
    private Long userId;
    private String startDate;
    private String endDate;
    private Integer pageNum = 1;
    private Integer pageSize = 20;
    private String orderBy = "createdAt";
    private Boolean asc = false;
}
