package com.dsai.rag.ingestion.service;

import com.dsai.rag.model.Chunk;
import com.dsai.rag.model.Document;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class ChunkingService {
    
    public List<Chunk> chunkDocument(Document document, int chunkSize, int chunkOverlap) {
        List<Chunk> chunks = new ArrayList<>();
        String content = document.getContent();
        
        if (content == null || content.isEmpty()) {
            return chunks;
        }
        
        String[] sentences = content.split("(?<=[.!?])\\s+");
        StringBuilder currentChunk = new StringBuilder();
        List<String> overlapBuffer = new ArrayList<>();
        int chunkIndex = 0;
        int currentPosition = 0;
        
        for (String sentence : sentences) {
            if (currentChunk.length() + sentence.length() > chunkSize && currentChunk.length() > 0) {
                String chunkContent = currentChunk.toString();
                Chunk chunk = createChunk(document, chunkContent, chunkIndex++, currentPosition);
                chunks.add(chunk);
                
                currentPosition += chunkContent.length();
                
                currentChunk = new StringBuilder();
                for (String overlap : overlapBuffer) {
                    currentChunk.append(overlap).append(" ");
                }
                
                overlapBuffer.clear();
            }
            
            currentChunk.append(sentence).append(" ");
            
            if (sentence.length() <= chunkOverlap) {
                overlapBuffer.add(sentence);
                if (overlapBuffer.stream().mapToInt(String::length).sum() > chunkOverlap) {
                    overlapBuffer.remove(0);
                }
            }
        }
        
        if (currentChunk.length() > 0) {
            String chunkContent = currentChunk.toString().trim();
            Chunk chunk = createChunk(document, chunkContent, chunkIndex, currentPosition);
            chunks.add(chunk);
        }
        
        log.debug("Created {} chunks from document {}", chunks.size(), document.getId());
        return chunks;
    }
    
    private Chunk createChunk(Document document, String content, int chunkIndex, int startPosition) {
        String chunkId = document.getId() + "_chunk_" + chunkIndex;
        
        return Chunk.builder()
                .id(chunkId)
                .documentId(document.getId())
                .content(content.trim())
                .chunkIndex(chunkIndex)
                .startPosition(startPosition)
                .endPosition(startPosition + content.length())
                .contentHash(DigestUtils.md5Hex(content))
                .type(Chunk.ChunkType.TEXT)
                .metadata(new HashMap<String, Object>() {{
                    put("source", document.getSource());
                    put("filename", document.getFilename());
                    put("documentType", document.getType().toString());
                    put("chunkIndex", chunkIndex);
                }})
                .build();
    }
}