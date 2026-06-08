package com.doccase.search.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReindexRequest {

    private String sourceIndex;
    private String targetIndex;

    @Builder.Default
    private int batchSize = 1000;

    @Builder.Default
    private String scrollTimeout = "5m";

    private boolean switchAliasOnComplete;
}
