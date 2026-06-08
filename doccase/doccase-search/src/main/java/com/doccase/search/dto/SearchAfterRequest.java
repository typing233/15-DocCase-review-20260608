package com.doccase.search.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchAfterRequest {

    private String keyword;
    private List<Long> tagIds;
    private String fileType;
    private Integer status;
    private String startDate;
    private String endDate;
    private String tenantId;

    private List<Object> searchAfter;
    private int pageSize;
    private String sortField;
    private String sortOrder;
}
