package com.dsai.rag.embeddings.service;

import com.dsai.rag.model.Chunk;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class EmbeddingService {
    
    private final EmbeddingModel embeddingModel;
    
    public EmbeddingService(
            @Value("${ollama.base-url:http://localhost:11434}") String ollamaBaseUrl,
            @Value("${ollama.embedding-model:nomic-embed-text}") String modelName,
            @Value("${ollama.timeout:60}") Integer timeout) {
        
        this.embeddingModel = OllamaEmbeddingModel.builder()
                .baseUrl(ollamaBaseUrl)
                .modelName(modelName)
                .timeout(Duration.ofSeconds(timeout))
                .build();
        
        log.info("Initialized Ollama embedding service with model: {} at {}", modelName, ollamaBaseUrl);
    }
    
    public List<Float> embedText(String text) {
        try {
            Embedding embedding = embeddingModel.embed(text).content();
            float[] vector = embedding.vector();
            List<Float> result = new ArrayList<>();
            for (float f : vector) {
                result.add(f);
            }
            return result;
        } catch (Exception e) {
            log.error("Failed to generate embedding for text", e);
            throw new RuntimeException("Embedding generation failed", e);
        }
    }
    
    public List<List<Float>> embedTexts(List<String> texts) {
        try {
            List<TextSegment> segments = texts.stream()
                    .map(TextSegment::from)
                    .collect(Collectors.toList());
            
            return embeddingModel.embedAll(segments).content()
                    .stream()
                    .map(embedding -> {
                        float[] vector = embedding.vector();
                        List<Float> result = new ArrayList<>();
                        for (float f : vector) {
                            result.add(f);
                        }
                        return result;
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Failed to generate embeddings for texts", e);
            throw new RuntimeException("Batch embedding generation failed", e);
        }
    }
    
    public Chunk embedChunk(Chunk chunk) {
        List<Float> embedding = embedText(chunk.getContent());
        chunk.setEmbedding(embedding);
        return chunk;
    }
    
    public List<Chunk> embedChunks(List<Chunk> chunks) {
        List<String> texts = chunks.stream()
                .map(Chunk::getContent)
                .collect(Collectors.toList());
        
        List<List<Float>> embeddings = embedTexts(texts);
        
        for (int i = 0; i < chunks.size(); i++) {
            chunks.get(i).setEmbedding(embeddings.get(i));
        }
        
        return chunks;
    }
}