package com.doccase.search.service.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.*;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.json.JsonData;
import com.doccase.common.domain.PageResult;
import com.doccase.search.document.DocumentIndex;
import com.doccase.search.dto.HybridSearchRequest;
import com.doccase.search.dto.SearchAfterRequest;
import com.doccase.search.service.EmbeddingService;
import com.doccase.search.service.SearchService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SearchServiceImpl implements SearchService {

    private final ElasticsearchClient esClient;
    private final EmbeddingService embeddingService;
    private final Timer searchTimer;
    private final Counter searchErrorCounter;
    private final Counter indexCounter;

    @Value("${elasticsearch.index-name:doccase_documents}")
    private String indexName;

    @Value("${elasticsearch.search.hybrid-alpha:0.7}")
    private float hybridAlpha;

    @Value("${elasticsearch.search.max-knn-candidates:100}")
    private int maxKnnCandidates;

    public SearchServiceImpl(ElasticsearchClient esClient, EmbeddingService embeddingService,
                             MeterRegistry meterRegistry) {
        this.esClient = esClient;
        this.embeddingService = embeddingService;
        this.searchTimer = Timer.builder("search.latency").register(meterRegistry);
        this.searchErrorCounter = Counter.builder("search.errors").register(meterRegistry);
        this.indexCounter = Counter.builder("search.index.operations").register(meterRegistry);
    }

    @Override
    @Retryable(retryFor = IOException.class, maxAttempts = 3, backoff = @Backoff(delay = 500, multiplier = 2))
    public PageResult<DocumentIndex> search(String keyword, List<Long> tagIds, String fileType,
                                            Integer status, String startDate, String endDate,
                                            int pageNum, int pageSize) {
        return searchTimer.record(() -> {
            try {
                BoolQuery.Builder boolBuilder = buildBaseQuery(keyword, tagIds, fileType, status, startDate, endDate);

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
                searchErrorCounter.increment();
                return PageResult.of(new ArrayList<>(), 0, pageNum, pageSize);
            }
        });
    }

    @Override
    @Retryable(retryFor = IOException.class, maxAttempts = 3, backoff = @Backoff(delay = 500, multiplier = 2))
    public PageResult<DocumentIndex> hybridSearch(HybridSearchRequest request) {
        return searchTimer.record(() -> {
            try {
                BoolQuery.Builder boolBuilder = buildBaseQuery(
                        request.getKeyword(), request.getTagIds(), request.getFileType(),
                        request.getStatus(), request.getStartDate(), request.getEndDate());

                if (request.getTenantId() != null) {
                    boolBuilder.filter(Query.of(q -> q.term(t -> t.field("tenantId").value(request.getTenantId()))));
                }

                int from = (request.getPageNum() - 1) * request.getPageSize();

                String semanticText = request.getSemanticQuery() != null ? request.getSemanticQuery() : request.getKeyword();
                float[] queryVector = null;
                if (semanticText != null && !semanticText.isBlank()) {
                    queryVector = embeddingService.generateEmbedding(semanticText);
                }

                SearchResponse<DocumentIndex> response;

                if (queryVector != null && hasNonZeroValues(queryVector)) {
                    final float[] vector = queryVector;
                    response = esClient.search(s -> s
                                    .index(indexName)
                                    .query(Query.of(q -> q.bool(boolBuilder.build())))
                                    .knn(knn -> knn
                                            .field("contentVector")
                                            .queryVector(toFloatList(vector))
                                            .k(request.getPageSize())
                                            .numCandidates(maxKnnCandidates)
                                    )
                                    .from(from)
                                    .size(request.getPageSize())
                                    .sort(sort -> sort.score(sc -> sc.order(SortOrder.Desc))),
                            DocumentIndex.class
                    );
                } else {
                    response = esClient.search(s -> s
                                    .index(indexName)
                                    .query(Query.of(q -> q.bool(boolBuilder.build())))
                                    .from(from)
                                    .size(request.getPageSize())
                                    .sort(sort -> sort.field(f -> f.field("createdAt").order(SortOrder.Desc))),
                            DocumentIndex.class
                    );
                }

                List<DocumentIndex> records = response.hits().hits().stream()
                        .map(Hit::source)
                        .collect(Collectors.toList());

                long total = response.hits().total() != null ? response.hits().total().value() : 0;
                return PageResult.of(records, total, request.getPageNum(), request.getPageSize());

            } catch (IOException e) {
                log.error("Hybrid search failed", e);
                searchErrorCounter.increment();
                return PageResult.of(new ArrayList<>(), 0, request.getPageNum(), request.getPageSize());
            }
        });
    }

    @Override
    public PageResult<DocumentIndex> semanticSearch(String query, String tenantId, int pageNum, int pageSize) {
        return searchTimer.record(() -> {
            try {
                float[] queryVector = embeddingService.generateEmbedding(query);
                if (!hasNonZeroValues(queryVector)) {
                    return PageResult.of(new ArrayList<DocumentIndex>(), 0L, pageNum, pageSize);
                }

                SearchResponse<DocumentIndex> response = esClient.search(s -> {
                    var search = s.index(indexName)
                            .knn(knn -> knn
                                    .field("contentVector")
                                    .queryVector(toFloatList(queryVector))
                                    .k(pageSize)
                                    .numCandidates(maxKnnCandidates)
                            )
                            .size(pageSize);

                    if (tenantId != null) {
                        search.query(Query.of(q -> q.term(t -> t.field("tenantId").value(tenantId))));
                    }
                    return search;
                }, DocumentIndex.class);

                List<DocumentIndex> records = response.hits().hits().stream()
                        .map(Hit::source)
                        .collect(Collectors.toList());

                long total = response.hits().total() != null ? response.hits().total().value() : 0;
                return PageResult.of(records, total, pageNum, pageSize);

            } catch (IOException e) {
                log.error("Semantic search failed", e);
                searchErrorCounter.increment();
                return PageResult.of(new ArrayList<>(), 0, pageNum, pageSize);
            }
        });
    }

    @Override
    public PageResult<DocumentIndex> searchAfter(SearchAfterRequest request) {
        return searchTimer.record(() -> {
            try {
                BoolQuery.Builder boolBuilder = buildBaseQuery(
                        request.getKeyword(), request.getTagIds(), request.getFileType(),
                        request.getStatus(), request.getStartDate(), request.getEndDate());

                if (request.getTenantId() != null) {
                    boolBuilder.filter(Query.of(q -> q.term(t -> t.field("tenantId").value(request.getTenantId()))));
                }

                String sortField = request.getSortField() != null ? request.getSortField() : "createdAt";
                SortOrder sortOrder = "asc".equalsIgnoreCase(request.getSortOrder()) ? SortOrder.Asc : SortOrder.Desc;

                SearchResponse<DocumentIndex> response = esClient.search(s -> {
                    var builder = s.index(indexName)
                            .query(Query.of(q -> q.bool(boolBuilder.build())))
                            .size(request.getPageSize())
                            .sort(sort -> sort.field(f -> f.field(sortField).order(sortOrder)))
                            .sort(sort -> sort.field(f -> f.field("id").order(SortOrder.Asc)));

                    if (request.getSearchAfter() != null && !request.getSearchAfter().isEmpty()) {
                        List<FieldValue> afterValues = request.getSearchAfter().stream()
                                .map(v -> FieldValue.of(v.toString()))
                                .toList();
                        builder.searchAfter(afterValues);
                    }
                    return builder;
                }, DocumentIndex.class);

                List<DocumentIndex> records = response.hits().hits().stream()
                        .map(Hit::source)
                        .collect(Collectors.toList());

                long total = response.hits().total() != null ? response.hits().total().value() : 0;
                return PageResult.of(records, total, 1, request.getPageSize());

            } catch (IOException e) {
                log.error("Search after failed", e);
                searchErrorCounter.increment();
                return PageResult.of(new ArrayList<>(), 0, 1, request.getPageSize());
            }
        });
    }

    @Override
    @Retryable(retryFor = IOException.class, maxAttempts = 3, backoff = @Backoff(delay = 500, multiplier = 2))
    public void indexDocument(DocumentIndex document) {
        try {
            IndexResponse response = esClient.index(i -> i
                    .index(indexName)
                    .id(document.getId().toString())
                    .routing(document.getTenantId())
                    .document(document)
            );
            indexCounter.increment();
            log.info("Document indexed: id={}, result={}", document.getId(), response.result());
        } catch (IOException e) {
            log.error("Failed to index document: id={}", document.getId(), e);
            throw new RuntimeException("Failed to index document", e);
        }
    }

    @Override
    public void indexDocumentWithEmbedding(DocumentIndex document) {
        String textForEmbedding = buildEmbeddingText(document);
        if (!textForEmbedding.isBlank()) {
            float[] embedding = embeddingService.generateEmbedding(textForEmbedding);
            document.setContentVector(embedding);
        }
        indexDocument(document);
    }

    @Override
    public void bulkIndex(List<DocumentIndex> documents) {
        if (documents == null || documents.isEmpty()) return;

        try {
            BulkRequest.Builder bulkBuilder = new BulkRequest.Builder();
            for (DocumentIndex doc : documents) {
                bulkBuilder.operations(op -> op
                        .index(idx -> idx
                                .index(indexName)
                                .id(doc.getId().toString())
                                .routing(doc.getTenantId())
                                .document(doc)
                        )
                );
            }

            BulkResponse response = esClient.bulk(bulkBuilder.build());
            if (response.errors()) {
                for (BulkResponseItem item : response.items()) {
                    if (item.error() != null) {
                        log.error("Bulk index error for doc {}: {}", item.id(), item.error().reason());
                    }
                }
            }
            indexCounter.increment(documents.size());
            log.info("Bulk indexed {} documents", documents.size());
        } catch (IOException e) {
            log.error("Bulk index failed for {} documents", documents.size(), e);
            throw new RuntimeException("Bulk index failed", e);
        }
    }

    @Override
    @Retryable(retryFor = IOException.class, maxAttempts = 3, backoff = @Backoff(delay = 500, multiplier = 2))
    public void updateDocument(DocumentIndex document) {
        try {
            UpdateResponse<DocumentIndex> response = esClient.update(u -> u
                            .index(indexName)
                            .id(document.getId().toString())
                            .routing(document.getTenantId())
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
    @Retryable(retryFor = IOException.class, maxAttempts = 3, backoff = @Backoff(delay = 500, multiplier = 2))
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

    private BoolQuery.Builder buildBaseQuery(String keyword, List<Long> tagIds, String fileType,
                                             Integer status, String startDate, String endDate) {
        BoolQuery.Builder boolBuilder = new BoolQuery.Builder();

        if (keyword != null && !keyword.isBlank()) {
            boolBuilder.must(Query.of(q -> q.multiMatch(mm -> mm
                    .fields("title^3", "description^2", "fileName", "ocrText", "tagNames")
                    .query(keyword)
                    .fuzziness("AUTO")
            )));
        }

        if (tagIds != null && !tagIds.isEmpty()) {
            List<FieldValue> tagFieldValues = tagIds.stream()
                    .map(id -> FieldValue.of(id))
                    .collect(Collectors.toList());
            boolBuilder.filter(Query.of(q -> q.terms(t -> t
                    .field("tagIds")
                    .terms(tv -> tv.value(tagFieldValues))
            )));
        }

        if (fileType != null && !fileType.isBlank()) {
            boolBuilder.filter(Query.of(q -> q.term(t -> t.field("fileType").value(fileType))));
        }

        if (status != null) {
            boolBuilder.filter(Query.of(q -> q.term(t -> t.field("status").value(status))));
        }

        if (startDate != null || endDate != null) {
            boolBuilder.filter(Query.of(q -> q.range(r -> {
                var rangeQuery = r.field("createdAt");
                if (startDate != null) rangeQuery.gte(JsonData.of(startDate));
                if (endDate != null) rangeQuery.lte(JsonData.of(endDate));
                return rangeQuery;
            })));
        }

        return boolBuilder;
    }

    private String buildEmbeddingText(DocumentIndex doc) {
        StringBuilder sb = new StringBuilder();
        if (doc.getTitle() != null) sb.append(doc.getTitle()).append(" ");
        if (doc.getDescription() != null) sb.append(doc.getDescription()).append(" ");
        if (doc.getOcrText() != null) sb.append(doc.getOcrText().substring(0, Math.min(doc.getOcrText().length(), 500)));
        return sb.toString().trim();
    }

    private boolean hasNonZeroValues(float[] vector) {
        for (float v : vector) {
            if (v != 0.0f) return true;
        }
        return false;
    }

    private List<Float> toFloatList(float[] array) {
        List<Float> list = new ArrayList<>(array.length);
        for (float v : array) list.add(v);
        return list;
    }
}
