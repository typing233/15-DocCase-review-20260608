package com.doccase.tag.rule.domain.model;

import lombok.Data;

import java.util.List;

@Data
public class ConditionNode {

    private String operator;

    private String field;

    private String fieldOperator;

    private Object value;

    private List<ConditionNode> conditions;

    public boolean isGroup() {
        return "AND".equalsIgnoreCase(operator) || "OR".equalsIgnoreCase(operator) || "NOT".equalsIgnoreCase(operator);
    }

    public boolean isLeaf() {
        return field != null && fieldOperator != null;
    }
}
