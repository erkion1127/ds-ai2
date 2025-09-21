package com.dsai.rag.ingestion.service;

import com.dsai.rag.embeddings.service.EmbeddingService;
import com.dsai.rag.model.Chunk;
import com.dsai.rag.model.Document;
import com.dsai.rag.vectorstore.service.VectorStoreService;
import dev.langchain4j.data.document.DocumentParser;
import dev.langchain4j.data.document.parser.apache.tika.ApacheTikaDocumentParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentIngestionService {
    
    private final EmbeddingService embeddingService;
    private final VectorStoreService vectorStoreService;
    private final ChunkingService chunkingService;
    
    @Value("${ingestion.chunk-size:500}")
    private int chunkSize;
    
    @Value("${ingestion.chunk-overlap:100}")
    private int chunkOverlap;
    
    @Value("${ingestion.batch-size:10}")
    private int batchSize;
    
    private final DocumentParser documentParser = new ApacheTikaDocumentParser();
    
    public Document ingestDocument(Path filePath) {
        try {
            log.info("Starting ingestion for file: {}", filePath);
            
            String content = parseDocument(filePath);
            String contentHash = DigestUtils.sha256Hex(content);
            
            Document document = Document.builder()
                    .id(UUID.randomUUID().toString())
                    .filename(filePath.getFileName().toString())
                    .content(content)
                    .contentHash(contentHash)
                    .source(filePath.toString())
                    .type(detectDocumentType(filePath))
                    .size(Files.size(filePath))
                    .createdAt(LocalDateTime.now())
                    .status(Document.DocumentStatus.PROCESSING)
                    .version("1.0")
                    .metadata(new HashMap<>())
                    .build();
            
            List<Chunk> chunks = chunkingService.chunkDocument(document, chunkSize, chunkOverlap);
            
            chunks = embeddingService.embedChunks(chunks);
            
            processBatches(chunks);
            
            document.setStatus(Document.DocumentStatus.INDEXED);
            document.setUpdatedAt(LocalDateTime.now());
            
            log.info("Successfully ingested document: {} with {} chunks", document.getId(), chunks.size());
            return document;
            
        } catch (Exception e) {
            log.error("Failed to ingest document: {}", filePath, e);
            throw new RuntimeException("Document ingestion failed", e);
        }
    }
    
    private String parseDocument(Path filePath) throws Exception {
        try (InputStream inputStream = Files.newInputStream(filePath)) {
            dev.langchain4j.data.document.Document doc = documentParser.parse(inputStream);
            return doc.text();
        }
    }
    
    private Document.DocumentType detectDocumentType(Path filePath) {
        String filename = filePath.getFileName().toString().toLowerCase();
        if (filename.endsWith(".pdf")) return Document.DocumentType.PDF;
        if (filename.endsWith(".md")) return Document.DocumentType.MARKDOWN;
        if (filename.endsWith(".html") || filename.endsWith(".htm")) return Document.DocumentType.HTML;
        if (filename.endsWith(".docx")) return Document.DocumentType.DOCX;
        if (filename.endsWith(".txt")) return Document.DocumentType.TXT;
        if (filename.endsWith(".json")) return Document.DocumentType.JSON;
        if (filename.endsWith(".xml")) return Document.DocumentType.XML;
        return Document.DocumentType.TXT;
    }
    
    private void processBatches(List<Chunk> chunks) {
        for (int i = 0; i < chunks.size(); i += batchSize) {
            int end = Math.min(i + batchSize, chunks.size());
            List<Chunk> batch = chunks.subList(i, end);
            vectorStoreService.upsertBatch(batch);
            log.debug("Processed batch {}/{}", i/batchSize + 1, (chunks.size() + batchSize - 1)/batchSize);
        }
    }
    
    public void deleteDocument(String documentId) {
        vectorStoreService.deleteByDocumentId(documentId);
        log.info("Deleted all chunks for document: {}", documentId);
    }
}