package com.doccase.search.service.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Time;
import co.elastic.clients.elasticsearch.core.*;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.indices.*;
import co.elastic.clients.elasticsearch.indices.get_alias.IndexAliases;
import com.doccase.common.constant.RedisKeyConstants;
import com.doccase.common.util.DistributedLockUtil;
import com.doccase.search.document.DocumentIndex;
import com.doccase.search.dto.IndexStatus;
import com.doccase.search.dto.ReindexRequest;
import com.doccase.search.service.IndexManagementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
@RequiredArgsConstructor
public class IndexManagementServiceImpl implements IndexManagementService {

    private final ElasticsearchClient esClient;
    private final StringRedisTemplate redisTemplate;
    private final DistributedLockUtil distributedLockUtil;

    @Value("${elasticsearch.alias-name:doccase_documents}")
    private String aliasName;

    private final AtomicBoolean reindexCancelled = new AtomicBoolean(false);

    @Override
    public void createIndexWithMapping(String indexName, String mappingJson) {
        try {
            boolean exists = esClient.indices().exists(e -> e.index(indexName)).value();
            if (exists) {
                log.info("Index {} already exists, skipping creation", indexName);
                return;
            }

            esClient.indices().create(c -> c
                    .index(indexName)
                    .withJson(new StringReader(mappingJson))
            );
            log.info("Created index {} with mapping", indexName);
        } catch (IOException e) {
            log.error("Failed to create index {}", indexName, e);
            throw new RuntimeException("Failed to create index", e);
        }
    }

    @Override
    @Async
    public void reindex(ReindexRequest request) {
        String lockKey = RedisKeyConstants.SEARCH_REINDEX_LOCK;
        distributedLockUtil.executeWithLock(lockKey, 5, 3600, java.util.concurrent.TimeUnit.SECONDS, () -> {
            doReindex(request);
        });
    }

    private void doReindex(ReindexRequest request) {
        reindexCancelled.set(false);
        String sourceIndex = request.getSourceIndex();
        String targetIndex = request.getTargetIndex();
        int batchSize = request.getBatchSize();

        log.info("Starting reindex from {} to {}, batch size: {}", sourceIndex, targetIndex, batchSize);
        redisTemplate.opsForValue().set(RedisKeyConstants.SEARCH_REINDEX_PROGRESS, "0");

        try {
            long totalDocs = getDocumentCount(sourceIndex);
            long processed = 0;

            SearchResponse<DocumentIndex> initialResponse = esClient.search(s -> s
                            .index(sourceIndex)
                            .size(batchSize)
                            .scroll(Time.of(t -> t.time(request.getScrollTimeout())))
                            .sort(sort -> sort.field(f -> f.field("_doc"))),
                    DocumentIndex.class
            );

            String scrollId = initialResponse.scrollId();
            List<Hit<DocumentIndex>> hits = initialResponse.hits().hits();

            while (hits.size() > 0 && !reindexCancelled.get()) {
                BulkRequest.Builder bulkBuilder = new BulkRequest.Builder();
                for (Hit<DocumentIndex> hit : hits) {
                    if (hit.source() != null) {
                        DocumentIndex doc = hit.source();
                        bulkBuilder.operations(op -> op
                                .index(idx -> idx
                                        .index(targetIndex)
                                        .id(hit.id())
                                        .document(doc)
                                )
                        );
                    }
                }

                BulkResponse bulkResponse = esClient.bulk(bulkBuilder.build());
                if (bulkResponse.errors()) {
                    log.warn("Bulk indexing had errors during reindex");
                }

                processed += hits.size();
                int progress = totalDocs > 0 ? (int) ((processed * 100) / totalDocs) : 0;
                redisTemplate.opsForValue().set(RedisKeyConstants.SEARCH_REINDEX_PROGRESS, String.valueOf(progress));
                log.info("Reindex progress: {}/{} ({}%)", processed, totalDocs, progress);

                String currentScrollId = scrollId;
                ScrollResponse<DocumentIndex> scrollResp = esClient.scroll(sr -> sr
                                .scrollId(currentScrollId)
                                .scroll(Time.of(t -> t.time(request.getScrollTimeout()))),
                        DocumentIndex.class
                );
                scrollId = scrollResp.scrollId();
                hits = scrollResp.hits().hits();
            }

            String finalScrollId = scrollId;
            esClient.clearScroll(cs -> cs.scrollId(finalScrollId));

            if (!reindexCancelled.get()) {
                redisTemplate.opsForValue().set(RedisKeyConstants.SEARCH_REINDEX_PROGRESS, "100");
                log.info("Reindex completed: {} documents processed", processed);

                if (request.isSwitchAliasOnComplete()) {
                    switchAlias(aliasName, targetIndex);
                }
            } else {
                log.info("Reindex cancelled after processing {} documents", processed);
            }

        } catch (IOException e) {
            log.error("Reindex failed", e);
            redisTemplate.opsForValue().set(RedisKeyConstants.SEARCH_REINDEX_PROGRESS, "-1");
            throw new RuntimeException("Reindex failed", e);
        }
    }

    @Override
    public void switchAlias(String aliasName, String targetIndex) {
        try {
            GetAliasResponse aliasResponse = esClient.indices().getAlias(g -> g.name(aliasName));
            Map<String, IndexAliases> aliases = aliasResponse.result();

            UpdateAliasesRequest.Builder updateBuilder = new UpdateAliasesRequest.Builder();

            for (String existingIndex : aliases.keySet()) {
                updateBuilder.actions(a -> a.remove(r -> r.index(existingIndex).alias(aliasName)));
            }
            updateBuilder.actions(a -> a.add(ad -> ad.index(targetIndex).alias(aliasName)));

            esClient.indices().updateAliases(updateBuilder.build());
            log.info("Switched alias {} to index {}", aliasName, targetIndex);
        } catch (IOException e) {
            log.error("Failed to switch alias {} to {}", aliasName, targetIndex, e);
            throw new RuntimeException("Failed to switch alias", e);
        }
    }

    @Override
    public IndexStatus getStatus() {
        try {
            String progressStr = redisTemplate.opsForValue().get(RedisKeyConstants.SEARCH_REINDEX_PROGRESS);
            int progress = progressStr != null ? Integer.parseInt(progressStr) : -1;
            boolean reindexing = progress >= 0 && progress < 100;

            GetAliasResponse aliasResponse = esClient.indices().getAlias(g -> g.name(aliasName));
            String currentIndex = aliasResponse.result().keySet().stream().findFirst().orElse("unknown");

            long docCount = getDocumentCount(currentIndex);

            return IndexStatus.builder()
                    .currentIndex(currentIndex)
                    .aliasName(aliasName)
                    .documentCount(docCount)
                    .healthStatus("green")
                    .reindexInProgress(reindexing)
                    .reindexProgressPercent(Math.max(progress, 0))
                    .build();
        } catch (IOException e) {
            log.error("Failed to get index status", e);
            return IndexStatus.builder()
                    .aliasName(aliasName)
                    .healthStatus("unknown")
                    .build();
        }
    }

    @Override
    public void cancelReindex() {
        reindexCancelled.set(true);
        redisTemplate.delete(RedisKeyConstants.SEARCH_REINDEX_PROGRESS);
        log.info("Reindex cancellation requested");
    }

    @Override
    public long getDocumentCount(String indexName) {
        try {
            var countResponse = esClient.count(c -> c.index(indexName));
            return countResponse.count();
        } catch (IOException e) {
            log.error("Failed to get document count for {}", indexName, e);
            return 0;
        }
    }
}
