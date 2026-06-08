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
public class HybridSearchRequest {

    private String keyword;
    private String semanticQuery;
    private List<Long> tagIds;
    private String fileType;
    private Integer status;
    private String startDate;
    private String endDate;
    private String tenantId;
    private Long userId;

    @Builder.Default
    private int pageNum = 1;
    @Builder.Default
    private int pageSize = 20;
    @Builder.Default
    private float keywordWeight = 0.7f;
    @Builder.Default
    private int knnCandidates = 100;
}
