package com.doccase.search.service.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.*;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.doccase.common.domain.PageResult;
import com.doccase.search.document.DocumentIndex;
import com.doccase.search.service.SearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {

    private final ElasticsearchClient esClient;

    @Value("${elasticsearch.index-name:doccase_documents}")
    private String indexName;

    @Override
    public PageResult<DocumentIndex> search(String keyword, List<Long> tagIds, String fileType,
                                            Integer status, String startDate, String endDate,
                                            int pageNum, int pageSize) {
        try {
            BoolQuery.Builder boolBuilder = new BoolQuery.Builder();

            // Multi-match for keyword across title, description, fileName, ocrText
            if (keyword != null && !keyword.isBlank()) {
                boolBuilder.must(Query.of(q -> q.multiMatch(mm -> mm
                        .fields("title^3", "description^2", "fileName", "ocrText", "tagNames")
                        .query(keyword)
                        .fuzziness("AUTO")
                )));
            }

            // Terms filter for tagIds
            if (tagIds != null && !tagIds.isEmpty()) {
                List<co.elastic.clients.elasticsearch._types.FieldValue> tagFieldValues = tagIds.stream()
                        .map(id -> co.elastic.clients.elasticsearch._types.FieldValue.of(id))
                        .collect(Collectors.toList());
                boolBuilder.filter(Query.of(q -> q.terms(t -> t
                        .field("tagIds")
                        .terms(tv -> tv.value(tagFieldValues))
                )));
            }

            // Term filter for fileType
            if (fileType != null && !fileType.isBlank()) {
                boolBuilder.filter(Query.of(q -> q.term(t -> t
                        .field("fileType")
                        .value(fileType)
                )));
            }

            // Term filter for status
            if (status != null) {
                boolBuilder.filter(Query.of(q -> q.term(t -> t
                        .field("status")
                        .value(status)
                )));
            }

            // Range filter for dates
            if (startDate != null || endDate != null) {
                boolBuilder.filter(Query.of(q -> q.range(r -> {
                    var rangeQuery = r.field("createdAt");
                    if (startDate != null) {
                        rangeQuery.gte(co.elastic.clients.json.JsonData.of(startDate));
                    }
                    if (endDate != null) {
                        rangeQuery.lte(co.elastic.clients.json.JsonData.of(endDate));
                    }
                    return rangeQuery;
                })));
            }

            int from = (pageNum - 1) * pageSize;

            SearchResponse<DocumentIndex> response = esClient.search(s -> s
                            .index(indexName)
                            .query(Query.of(q -> q.bool(boolBuilder.build())))
                            .from(from)
                            .size(pageSize)
                            .sort(sort -> sort.field(f -> f.field("createdAt").order(SortOrder.Desc))),
                    DocumentIndex.class
            );

            List<DocumentIndex> records = response.hits().hits().stream()
                    .map(Hit::source)
                    .collect(Collectors.toList());

            long total = response.hits().total() != null ? response.hits().total().value() : 0;

            return PageResult.of(records, total, pageNum, pageSize);

        } catch (IOException e) {
            log.error("Elasticsearch search failed", e);
            return PageResult.of(new ArrayList<>(), 0, pageNum, pageSize);
        }
    }

    @Override
    public void indexDocument(DocumentIndex document) {
        try {
            IndexResponse response = esClient.index(i -> i
                    .index(indexName)
                    .id(document.getId().toString())
                    .document(document)
            );
            log.info("Document indexed successfully: id={}, result={}", document.getId(), response.result());
        } catch (IOException e) {
            log.error("Failed to index document: id={}", document.getId(), e);
            throw new RuntimeException("Failed to index document", e);
        }
    }

    @Override
    public void updateDocument(DocumentIndex document) {
        try {
            UpdateResponse<DocumentIndex> response = esClient.update(u -> u
                            .index(indexName)
                            .id(document.getId().toString())
                            .doc(document)
                            .docAsUpsert(true),
                    DocumentIndex.class
            );
            log.info("Document updated in index: id={}, result={}", document.getId(), response.result());
        } catch (IOException e) {
            log.error("Failed to update document in index: id={}", document.getId(), e);
            throw new RuntimeException("Failed to update document index", e);
        }
    }

    @Override
    public void deleteDocument(Long documentId) {
        try {
            DeleteResponse response = esClient.delete(d -> d
                    .index(indexName)
                    .id(documentId.toString())
            );
            log.info("Document deleted from index: id={}, result={}", documentId, response.result());
        } catch (IOException e) {
            log.error("Failed to delete document from index: id={}", documentId, e);
            throw new RuntimeException("Failed to delete document from index", e);
        }
    }
}
