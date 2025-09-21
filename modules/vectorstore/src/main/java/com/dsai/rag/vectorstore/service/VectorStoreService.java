package com.dsai.rag.vectorstore.service;

import com.dsai.rag.model.Chunk;
import com.dsai.rag.model.QueryRequest;

import java.util.List;
import java.util.Map;

public interface VectorStoreService {
    void upsert(Chunk chunk);
    void upsertBatch(List<Chunk> chunks);
    List<Chunk> search(String query, List<Float> queryEmbedding, int topK, Map<String, Object> filters);
    List<Chunk> hybridSearch(String query, List<Float> queryEmbedding, int topK, Map<String, Object> filters);
    void delete(String chunkId);
    void deleteByDocumentId(String documentId);
    boolean exists(String chunkId);
    long count();
    void createCollection(String collectionName);
    void deleteCollection(String collectionName);
}