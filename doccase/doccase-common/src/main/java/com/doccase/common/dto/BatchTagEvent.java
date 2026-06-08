package com.doccase.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchTagEvent implements Serializable {

    private String tenantId;
    private Long operatorId;
    private String action;
    private List<Long> documentIds;
    private List<Long> tagIds;
    private Long targetParentId;
}
