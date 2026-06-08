package com.doccase.common.constant;

public final class MqConstants {

    private MqConstants() {}

    // Exchanges
    public static final String EXCHANGE_DOCUMENT = "doccase.document";
    public static final String EXCHANGE_TAG = "doccase.tag";
    public static final String EXCHANGE_OCR = "doccase.ocr";
    public static final String EXCHANGE_EXPORT = "doccase.export";
    public static final String EXCHANGE_EMAIL = "doccase.email";
    public static final String EXCHANGE_DLX = "doccase.dlx";

    // Routing Keys - Document
    public static final String RK_DOCUMENT_CREATED = "document.created";
    public static final String RK_DOCUMENT_UPDATED = "document.updated";
    public static final String RK_DOCUMENT_DELETED = "document.deleted";

    // Routing Keys - Tag
    public static final String RK_TAG_UPDATED = "tag.updated";
    public static final String RK_TAG_MERGED = "tag.merged";
    public static final String RK_TAG_BATCH_COMPLETED = "tag.batch.completed";

    // Routing Keys - OCR
    public static final String RK_OCR_SUBMIT = "ocr.submit";
    public static final String RK_OCR_COMPLETED = "ocr.completed";
    public static final String RK_OCR_FAILED = "ocr.failed";

    // Routing Keys - Export
    public static final String RK_EXPORT_REQUEST = "export.request";

    // Routing Keys - Email
    public static final String RK_EMAIL_ATTACHMENT_ARCHIVED = "email.attachment.archived";
    public static final String RK_EMAIL_POLL_COMPLETED = "email.poll.completed";
    public static final String RK_EMAIL_POLL_FAILED = "email.poll.failed";

    // Routing Keys - Rule Engine
    public static final String RK_RULE_EVALUATE = "rule.evaluate";
    public static final String RK_RULE_EXECUTED = "rule.executed";

    // Queues - Search
    public static final String QUEUE_SEARCH_DOCUMENT = "search.document.queue";
    public static final String QUEUE_SEARCH_DOCUMENT_UPDATE = "search.document.update.queue";
    public static final String QUEUE_SEARCH_DOCUMENT_DELETE = "search.document.delete.queue";
    public static final String QUEUE_SEARCH_TAG = "search.tag.queue";

    // Queues - OCR
    public static final String QUEUE_OCR_TASK = "ocr.task.queue";
    public static final String QUEUE_OCR_RESULT = "ocr.result.queue";

    // Queues - Export
    public static final String QUEUE_EXPORT = "export.task.queue";

    // Queues - Tag
    public static final String QUEUE_TAG_BATCH = "tag.batch.queue";
    public static final String QUEUE_TAG_DOCUMENT_COUNT = "tag.document.count.queue";
    public static final String QUEUE_TAG_DOCUMENT_DELETE = "tag.document.delete.queue";

    // Queues - Rule Engine
    public static final String QUEUE_RULE_EVALUATE = "rule.evaluate.queue";

    // Queues - Email
    public static final String QUEUE_EMAIL_ARCHIVE = "email.archive.queue";
    public static final String QUEUE_EMAIL_DLQ = "email.dlq.queue";

    // Queues - DLX
    public static final String QUEUE_DLX = "dlx.queue";
}
