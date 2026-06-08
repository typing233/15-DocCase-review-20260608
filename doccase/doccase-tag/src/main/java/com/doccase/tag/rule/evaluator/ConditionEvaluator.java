package com.doccase.tag.rule.evaluator;

import com.doccase.common.dto.RuleEvaluateEvent;
import com.doccase.tag.rule.domain.model.ConditionNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

@Slf4j
@Component
public class ConditionEvaluator {

    private final ConcurrentHashMap<String, Pattern> regexCache = new ConcurrentHashMap<>();

    public boolean evaluate(ConditionNode node, RuleEvaluateEvent context) {
        if (node == null) return false;

        if (node.isGroup()) {
            return evaluateGroup(node, context);
        } else if (node.isLeaf()) {
            return evaluateLeaf(node, context);
        }

        return false;
    }

    private boolean evaluateGroup(ConditionNode node, RuleEvaluateEvent context) {
        List<ConditionNode> children = node.getConditions();
        if (children == null || children.isEmpty()) return true;

        return switch (node.getOperator().toUpperCase()) {
            case "AND" -> children.stream().allMatch(c -> evaluate(c, context));
            case "OR" -> children.stream().anyMatch(c -> evaluate(c, context));
            case "NOT" -> !evaluate(children.get(0), context);
            default -> false;
        };
    }

    private boolean evaluateLeaf(ConditionNode node, RuleEvaluateEvent context) {
        Object fieldValue = extractFieldValue(node.getField(), context);
        Object conditionValue = node.getValue();
        String operator = node.getFieldOperator();

        if ("EXISTS".equalsIgnoreCase(operator)) {
            return fieldValue != null && !fieldValue.toString().isEmpty();
        }
        if ("NOT_EXISTS".equalsIgnoreCase(operator)) {
            return fieldValue == null || fieldValue.toString().isEmpty();
        }

        if (fieldValue == null) return false;

        String fieldStr = fieldValue.toString();
        String condStr = conditionValue != null ? conditionValue.toString() : "";

        return switch (operator.toUpperCase()) {
            case "EQUALS" -> fieldStr.equals(condStr);
            case "NOT_EQUALS" -> !fieldStr.equals(condStr);
            case "CONTAINS" -> fieldStr.contains(condStr);
            case "NOT_CONTAINS" -> !fieldStr.contains(condStr);
            case "STARTS_WITH" -> fieldStr.startsWith(condStr);
            case "ENDS_WITH" -> fieldStr.endsWith(condStr);
            case "REGEX" -> matchRegex(fieldStr, condStr);
            case "IN" -> evaluateIn(fieldStr, conditionValue);
            case "GT" -> compareNumeric(fieldStr, condStr) > 0;
            case "GTE" -> compareNumeric(fieldStr, condStr) >= 0;
            case "LT" -> compareNumeric(fieldStr, condStr) < 0;
            case "LTE" -> compareNumeric(fieldStr, condStr) <= 0;
            default -> false;
        };
    }

    @SuppressWarnings("unchecked")
    private Object extractFieldValue(String field, RuleEvaluateEvent context) {
        if (field == null) return null;

        return switch (field) {
            case "title" -> context.getTitle();
            case "description" -> context.getDescription();
            case "fileName" -> context.getFileName();
            case "fileType" -> context.getFileType();
            case "mimeType" -> context.getMimeType();
            case "fileSize" -> context.getFileSize();
            case "ocrText" -> context.getOcrText();
            case "userId" -> context.getUserId();
            case "tagNames" -> context.getTagNames() != null ? String.join(",", context.getTagNames()) : null;
            default -> {
                if (field.startsWith("metadata.") && context.getMetadata() != null) {
                    String metaKey = field.substring("metadata.".length());
                    Object val = context.getMetadata().get(metaKey);
                    yield val != null ? val.toString() : null;
                }
                yield null;
            }
        };
    }

    private boolean matchRegex(String value, String pattern) {
        try {
            Pattern compiled = regexCache.computeIfAbsent(pattern, Pattern::compile);
            return compiled.matcher(value).find();
        } catch (PatternSyntaxException e) {
            log.warn("Invalid regex pattern: {}", pattern);
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    private boolean evaluateIn(String value, Object conditionValue) {
        if (conditionValue instanceof List<?> list) {
            return list.stream().anyMatch(item -> value.equals(item.toString()));
        }
        if (conditionValue instanceof String csv) {
            for (String item : csv.split(",")) {
                if (value.equals(item.trim())) return true;
            }
        }
        return false;
    }

    private int compareNumeric(String a, String b) {
        try {
            double numA = Double.parseDouble(a);
            double numB = Double.parseDouble(b);
            return Double.compare(numA, numB);
        } catch (NumberFormatException e) {
            return a.compareTo(b);
        }
    }

    public boolean validateConditionTree(ConditionNode node) {
        if (node == null) return false;

        if (node.isGroup()) {
            if (node.getConditions() == null || node.getConditions().isEmpty()) return false;
            return node.getConditions().stream().allMatch(this::validateConditionTree);
        }

        if (node.isLeaf()) {
            if ("REGEX".equalsIgnoreCase(node.getFieldOperator())) {
                try {
                    Pattern.compile(node.getValue().toString());
                } catch (PatternSyntaxException e) {
                    return false;
                }
            }
            return true;
        }

        return false;
    }
}
