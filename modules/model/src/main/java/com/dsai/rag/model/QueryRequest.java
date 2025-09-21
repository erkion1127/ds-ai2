package com.dsai.rag.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueryRequest {
    private String query;
    private Integer topK;
    private Double minScore;
    private SearchStrategy strategy;
    private Map<String, Object> filters;
    private Boolean includeMetadata;
    private String modelName;
    private Double temperature;
    private Integer maxTokens;
    
    public enum SearchStrategy {
        VECTOR_ONLY, HYBRID, BM25_ONLY
    }
}