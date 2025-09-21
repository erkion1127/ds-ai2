package com.dsai.rag.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Document {
    private String id;
    private String filename;
    private String content;
    private String contentHash;
    private String source;
    private DocumentType type;
    private Long size;
    private Map<String, Object> metadata;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String version;
    private DocumentStatus status;
    
    public enum DocumentType {
        PDF, MARKDOWN, HTML, DOCX, TXT, JSON, XML
    }
    
    public enum DocumentStatus {
        PENDING, PROCESSING, INDEXED, FAILED, DELETED
    }
}