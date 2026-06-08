package com.doccase.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailArchiveEvent implements Serializable {

    private Long accountId;
    private String tenantId;
    private String messageId;
    private Long messageUid;
    private String fromAddress;
    private String subject;
    private String attachmentFileName;
    private String attachmentHash;
    private Long attachmentSize;
    private String attachmentMimeType;
    private Long documentId;
    private LocalDateTime archivedAt;
}
