package com.doccase.search.service;

import com.doccase.common.domain.PageResult;
import com.doccase.search.document.DocumentIndex;

import java.util.List;
import java.util.Map;

public interface SearchService {

    PageResult<DocumentIndex> search(String keyword, List<Long> tagIds, String fileType,
                                     Integer status, String startDate, String endDate,
                                     int pageNum, int pageSize);

    void indexDocument(DocumentIndex document);

    void updateDocument(DocumentIndex document);

    void deleteDocument(Long documentId);
}
