package com.dsai.rag.vectorstore.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import co.elastic.clients.elasticsearch.core.*;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.json.JsonData;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.dsai.rag.model.Chunk;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ElasticsearchVectorStore implements VectorStoreService {
    
    private final ElasticsearchClient client;
    private final String indexName;
    private final ObjectMapper objectMapper;
    
    public ElasticsearchVectorStore(
            @Value("${elasticsearch.host:localhost}") String host,
            @Value("${elasticsearch.port:9200}") int port,
            @Value("${elasticsearch.index:rag-chunks}") String indexName) {
        
        RestClient restClient = RestClient.builder(
                new HttpHost(host, port, "http")
        ).build();
        
        this.objectMapper = new ObjectMapper();
        ElasticsearchTransport transport = new RestClientTransport(
                restClient, new JacksonJsonpMapper(objectMapper)
        );
        
        this.client = new ElasticsearchClient(transport);
        this.indexName = indexName;
        
        log.info("Initialized Elasticsearch vector store at {}:{} with index: {}", host, port, indexName);
    }
    
    @PostConstruct
    public void init() {
        try {
            // Test connection first
            client.ping();
            
            if (!client.indices().exists(e -> e.index(indexName)).value()) {
                // Index will be auto-created with dynamic mapping
                log.info("Index {} will be created with dynamic mapping on first document", indexName);
            } else {
                log.info("Index {} already exists", indexName);
            }
        } catch (Exception e) {
            log.warn("Elasticsearch is not available at initialization. Will retry on first use: {}", e.getMessage());
            // Don't throw exception - allow the service to start
        }
    }
    
    @Override
    public void upsert(Chunk chunk) {
        try {
            Map<String, Object> document = convertChunkToMap(chunk);
            
            client.index(i -> i
                    .index(indexName)
                    .id(chunk.getId())
                    .document(document)
            );
            
            log.debug("Upserted chunk: {}", chunk.getId());
        } catch (IOException e) {
            log.error("Failed to upsert chunk", e);
            throw new RuntimeException("Failed to upsert chunk", e);
        }
    }
    
    @Override
    public void upsertBatch(List<Chunk> chunks) {
        try {
            BulkRequest.Builder bulkBuilder = new BulkRequest.Builder();
            
            for (Chunk chunk : chunks) {
                Map<String, Object> document = convertChunkToMap(chunk);
                bulkBuilder.operations(op -> op
                        .index(idx -> idx
                                .index(indexName)
                                .id(chunk.getId())
                                .document(document)
                        )
                );
            }
            
            BulkResponse result = client.bulk(bulkBuilder.build());
            
            if (result.errors()) {
                log.error("Bulk upsert had errors");
            } else {
                log.info("Successfully upserted {} chunks", chunks.size());
            }
        } catch (IOException e) {
            log.error("Failed to bulk upsert chunks", e);
            throw new RuntimeException("Failed to bulk upsert chunks", e);
        }
    }
    
    @Override
    public List<Chunk> search(String query, List<Float> queryEmbedding, int topK, Map<String, Object> filters) {
        try {
            // Check if Elasticsearch is available
            try {
                client.ping();
            } catch (Exception e) {
                log.warn("Elasticsearch is not available for search: {}", e.getMessage());
                return new ArrayList<>(); // Return empty list if ES is down
            }
            
            // Use script_score query for vector similarity search
            Query scriptScoreQuery = ScriptScoreQuery.of(s -> s
                    .query(MatchAllQuery.of(m -> m)._toQuery())
                    .script(sc -> sc
                            .inline(i -> i
                                    .source("cosineSimilarity(params.query_vector, 'embedding') + 1.0")
                                    .params("query_vector", JsonData.of(queryEmbedding))
                            )
                    )
                    .minScore(0.0f)
            )._toQuery();
            
            SearchResponse<Map> response = client.search(s -> s
                    .index(indexName)
                    .query(scriptScoreQuery)
                    .size(topK),
                    Map.class
            );
            
            return response.hits().hits().stream()
                    .map(this::mapHitToChunk)
                    .collect(Collectors.toList());
            
        } catch (IOException e) {
            log.error("Failed to search chunks", e);
            throw new RuntimeException("Failed to search chunks", e);
        }
    }
    
    @Override
    public List<Chunk> hybridSearch(String query, List<Float> queryEmbedding, int topK, Map<String, Object> filters) {
        try {
            // Check if Elasticsearch is available
            try {
                client.ping();
            } catch (Exception e) {
                log.warn("Elasticsearch is not available for hybrid search: {}", e.getMessage());
                return new ArrayList<>(); // Return empty list if ES is down
            }
            Query textQuery = MatchQuery.of(m -> m
                    .field("content")
                    .query(query)
            )._toQuery();
            
            // For hybrid search, combine script_score with text search
            Query scriptScoreQuery = ScriptScoreQuery.of(s -> s
                    .query(MatchAllQuery.of(m -> m)._toQuery())
                    .script(sc -> sc
                            .inline(i -> i
                                    .source("cosineSimilarity(params.query_vector, 'embedding') + 1.0")
                                    .params("query_vector", JsonData.of(queryEmbedding))
                            )
                    )
                    .minScore(0.0f)
            )._toQuery();
            
            Query hybridQuery = BoolQuery.of(b -> b
                    .should(textQuery, scriptScoreQuery)
                    .minimumShouldMatch("1")
            )._toQuery();
            
            SearchResponse<Map> response = client.search(s -> s
                    .index(indexName)
                    .query(hybridQuery)
                    .size(topK),
                    Map.class
            );
            
            return response.hits().hits().stream()
                    .map(this::mapHitToChunk)
                    .collect(Collectors.toList());
            
        } catch (IOException e) {
            log.error("Failed to perform hybrid search", e);
            throw new RuntimeException("Failed to perform hybrid search", e);
        }
    }
    
    @Override
    public void delete(String chunkId) {
        try {
            client.delete(d -> d
                    .index(indexName)
                    .id(chunkId)
            );
            log.debug("Deleted chunk: {}", chunkId);
        } catch (IOException e) {
            log.error("Failed to delete chunk", e);
        }
    }
    
    @Override
    public void deleteByDocumentId(String documentId) {
        try {
            client.deleteByQuery(d -> d
                    .index(indexName)
                    .query(q -> q
                            .term(t -> t
                                    .field("documentId")
                                    .value(documentId)
                            )
                    )
            );
            log.info("Deleted all chunks for document: {}", documentId);
        } catch (IOException e) {
            log.error("Failed to delete chunks by document ID", e);
        }
    }
    
    @Override
    public boolean exists(String chunkId) {
        try {
            return client.exists(e -> e
                    .index(indexName)
                    .id(chunkId)
            ).value();
        } catch (IOException e) {
            log.error("Failed to check existence", e);
            return false;
        }
    }
    
    @Override
    public long count() {
        try {
            return client.count(c -> c.index(indexName)).count();
        } catch (IOException e) {
            log.error("Failed to count documents", e);
            return 0;
        }
    }
    
    @Override
    public void createCollection(String collectionName) {
        try {
            client.indices().create(c -> c.index(collectionName));
        } catch (IOException e) {
            log.error("Failed to create collection", e);
        }
    }
    
    @Override
    public void deleteCollection(String collectionName) {
        try {
            client.indices().delete(d -> d.index(collectionName));
        } catch (IOException e) {
            log.error("Failed to delete collection", e);
        }
    }
    
    private Map<String, Object> convertChunkToMap(Chunk chunk) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", chunk.getId());
        map.put("documentId", chunk.getDocumentId());
        map.put("content", chunk.getContent());
        map.put("chunkIndex", chunk.getChunkIndex());
        map.put("embedding", chunk.getEmbedding());
        map.put("metadata", chunk.getMetadata());
        return map;
    }
    
    private Chunk mapHitToChunk(Hit<Map> hit) {
        Map source = hit.source();
        return Chunk.builder()
                .id((String) source.get("id"))
                .documentId((String) source.get("documentId"))
                .content((String) source.get("content"))
                .chunkIndex((Integer) source.get("chunkIndex"))
                .embedding((List<Float>) source.get("embedding"))
                .metadata((Map<String, Object>) source.get("metadata"))
                .build();
    }
}