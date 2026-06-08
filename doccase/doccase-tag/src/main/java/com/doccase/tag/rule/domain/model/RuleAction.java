package com.doccase.tag.rule.domain.model;

import lombok.Data;

@Data
public class RuleAction {

    private String type;

    private Long tagId;

    public enum ActionType {
        ADD_TAG, REMOVE_TAG
    }
}
