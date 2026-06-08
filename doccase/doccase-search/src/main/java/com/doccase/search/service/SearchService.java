package com.doccase.search.service;

import com.doccase.common.domain.PageResult;
import com.doccase.search.document.DocumentIndex;
import com.doccase.search.dto.HybridSearchRequest;
import com.doccase.search.dto.SearchAfterRequest;

import java.util.List;

public interface SearchService {

    PageResult<DocumentIndex> search(String keyword, List<Long> tagIds, String fileType,
                                     Integer status, String startDate, String endDate,
                                     int pageNum, int pageSize);

    PageResult<DocumentIndex> hybridSearch(HybridSearchRequest request);

    PageResult<DocumentIndex> semanticSearch(String query, String tenantId, int pageNum, int pageSize);

    PageResult<DocumentIndex> searchAfter(SearchAfterRequest request);

    void indexDocument(DocumentIndex document);

    void indexDocumentWithEmbedding(DocumentIndex document);

    void bulkIndex(List<DocumentIndex> documents);

    void updateDocument(DocumentIndex document);

    void deleteDocument(Long documentId);
}
