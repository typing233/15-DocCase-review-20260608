package com.doccase.document.service;

import com.doccase.common.domain.PageResult;
import com.doccase.common.dto.DocumentDTO;
import com.doccase.document.domain.vo.ChunkUploadRequest;
import com.doccase.document.domain.vo.DocumentCreateRequest;
import com.doccase.document.domain.vo.DocumentQueryRequest;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

public interface DocumentService {

    DocumentDTO createDocument(Long userId, DocumentCreateRequest request, MultipartFile file);

    DocumentDTO getDocument(Long id);

    PageResult<DocumentDTO> queryDocuments(DocumentQueryRequest request);

    DocumentDTO updateDocument(Long id, Long userId, Map<String, Object> updates);

    void deleteDocument(Long id, Long userId);

    void restoreVersion(Long documentId, Integer versionNumber, Long userId);
}
