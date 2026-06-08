package com.doccase.search.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IndexStatus {

    private String currentIndex;
    private String aliasName;
    private long documentCount;
    private String healthStatus;
    private int numberOfShards;
    private int numberOfReplicas;
    private boolean reindexInProgress;
    private int reindexProgressPercent;
    private String reindexSourceIndex;
    private String reindexTargetIndex;
}
