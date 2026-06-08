package com.doccase.tag.rule.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RuleDryRunResult {

    private Long ruleId;
    private String ruleName;
    private int totalDocuments;
    private int matchedDocuments;
    private List<MatchDetail> matches;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MatchDetail {
        private Long documentId;
        private String documentTitle;
        private boolean matched;
        private List<String> actionsToExecute;
    }
}
