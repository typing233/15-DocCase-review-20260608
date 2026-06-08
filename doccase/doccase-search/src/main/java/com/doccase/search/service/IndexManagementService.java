package com.doccase.search.service;

import com.doccase.search.dto.IndexStatus;
import com.doccase.search.dto.ReindexRequest;

public interface IndexManagementService {

    void createIndexWithMapping(String indexName, String mappingJson);

    void reindex(ReindexRequest request);

    void switchAlias(String aliasName, String targetIndex);

    IndexStatus getStatus();

    void cancelReindex();

    long getDocumentCount(String indexName);
}
