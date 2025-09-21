package com.dsai.rag.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Chunk {
    private String id;
    private String documentId;
    private String content;
    private Integer chunkIndex;
    private Integer startPosition;
    private Integer endPosition;
    private List<Float> embedding;
    private Map<String, Object> metadata;
    private String contentHash;
    private ChunkType type;
    
    public enum ChunkType {
        TEXT, CODE, TABLE, IMAGE_DESCRIPTION, METADATA
    }
}