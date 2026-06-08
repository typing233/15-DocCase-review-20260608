package com.doccase.common.constant;

public final class MqConstants {

    private MqConstants() {}

    // Exchanges
    public static final String EXCHANGE_DOCUMENT = "doccase.document";
    public static final String EXCHANGE_TAG = "doccase.tag";
    public static final String EXCHANGE_OCR = "doccase.ocr";
    public static final String EXCHANGE_EXPORT = "doccase.export";
    public static final String EXCHANGE_DLX = "doccase.dlx";

    // Routing Keys
    public static final String RK_DOCUMENT_CREATED = "document.created";
    public static final String RK_DOCUMENT_UPDATED = "document.updated";
    public static final String RK_DOCUMENT_DELETED = "document.deleted";

    public static final String RK_TAG_UPDATED = "tag.updated";
    public static final String RK_TAG_MERGED = "tag.merged";

    public static final String RK_OCR_SUBMIT = "ocr.submit";
    public static final String RK_OCR_COMPLETED = "ocr.completed";
    public static final String RK_OCR_FAILED = "ocr.failed";

    public static final String RK_EXPORT_REQUEST = "export.request";

    // Queues
    public static final String QUEUE_SEARCH_DOCUMENT = "search.document.queue";
    public static final String QUEUE_SEARCH_TAG = "search.tag.queue";
    public static final String QUEUE_OCR_TASK = "ocr.task.queue";
    public static final String QUEUE_OCR_RESULT = "ocr.result.queue";
    public static final String QUEUE_EXPORT = "export.task.queue";
    public static final String QUEUE_DLX = "dlx.queue";
}
