package com.dsai.rag.core.service;

import com.dsai.rag.embeddings.service.EmbeddingService;
import com.dsai.rag.model.Chunk;
import com.dsai.rag.model.QueryRequest;
import com.dsai.rag.vectorstore.service.VectorStoreService;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class RagOrchestrator {
    
    private final EmbeddingService embeddingService;
    private final VectorStoreService vectorStoreService;
    private final ChatLanguageModel chatModel;
    
    public RagOrchestrator(
            EmbeddingService embeddingService,
            VectorStoreService vectorStoreService,
            @Value("${ollama.base-url:http://localhost:11434}") String ollamaBaseUrl,
            @Value("${ollama.chat-model:llama3.2}") String chatModelName,
            @Value("${ollama.timeout:120}") Integer timeout) {
        
        this.embeddingService = embeddingService;
        this.vectorStoreService = vectorStoreService;
        
        this.chatModel = OllamaChatModel.builder()
                .baseUrl(ollamaBaseUrl)
                .modelName(chatModelName)
                .timeout(Duration.ofSeconds(timeout))
                .temperature(0.7)
                .build();
        
        log.info("Initialized RAG orchestrator with model: {}", chatModelName);
    }
    
    public RagResponse query(QueryRequest request) {
        long totalStartTime = System.currentTimeMillis();
        try {
            log.debug("Processing query: {}", request.getQuery());

            // 임베딩 생성 시간 측정
            long embeddingStartTime = System.currentTimeMillis();
            List<Float> queryEmbedding = embeddingService.embedText(request.getQuery());
            long embeddingTime = System.currentTimeMillis() - embeddingStartTime;
            log.info("[Performance] Embedding generation took {}ms", embeddingTime);

            // 벡터 검색 시간 측정
            long searchStartTime = System.currentTimeMillis();
            List<Chunk> retrievedChunks;
            if (request.getStrategy() == QueryRequest.SearchStrategy.HYBRID) {
                retrievedChunks = vectorStoreService.hybridSearch(
                        request.getQuery(),
                        queryEmbedding,
                        request.getTopK() != null ? request.getTopK() : 5,
                        request.getFilters()
                );
            } else {
                retrievedChunks = vectorStoreService.search(
                        request.getQuery(),
                        queryEmbedding,
                        request.getTopK() != null ? request.getTopK() : 5,
                        request.getFilters()
                );
            }
            long searchTime = System.currentTimeMillis() - searchStartTime;
            log.info("[Performance] Vector search took {}ms", searchTime);

            // Check if we have any retrieved chunks
            if (retrievedChunks == null || retrievedChunks.isEmpty()) {
                log.warn("No chunks retrieved, using direct LLM response");
                String directPrompt = "당신은 도움이 되는 AI 어시스턴트입니다. 다음 질문에 답변해주세요: " + request.getQuery();

                long llmStartTime = System.currentTimeMillis();
                String response = chatModel.generate(directPrompt);
                long llmTime = System.currentTimeMillis() - llmStartTime;
                log.info("[Performance] Direct LLM response took {}ms", llmTime);

                long totalTime = System.currentTimeMillis() - totalStartTime;
                log.info("[Performance] Total RAG query time: {}ms (Embedding: {}ms, Search: {}ms, LLM: {}ms)",
                        totalTime, embeddingTime, searchTime, llmTime);

                return RagResponse.builder()
                        .query(request.getQuery())
                        .answer(response)
                        .context("")
                        .sources(new ArrayList<>())
                        .retrievedChunks(0)
                        .build();
            }

            String context = buildContext(retrievedChunks);
            String prompt = buildPrompt(request.getQuery(), context);

            // LLM 응답 생성 시간 측정
            long llmStartTime = System.currentTimeMillis();
            String response = chatModel.generate(prompt);
            long llmTime = System.currentTimeMillis() - llmStartTime;
            log.info("[Performance] LLM response generation took {}ms", llmTime);

            long totalTime = System.currentTimeMillis() - totalStartTime;
            log.info("[Performance] Total RAG query time: {}ms (Embedding: {}ms, Search: {}ms, LLM: {}ms)",
                    totalTime, embeddingTime, searchTime, llmTime);

            return RagResponse.builder()
                    .query(request.getQuery())
                    .answer(response)
                    .context(context)
                    .sources(extractSources(retrievedChunks))
                    .retrievedChunks(retrievedChunks.size())
                    .build();
            
        } catch (Exception e) {
            log.error("Failed to process query: {}", e.getMessage(), e);
            throw new RuntimeException("Query processing failed: " + e.getMessage(), e);
        }
    }
    
    private String buildContext(List<Chunk> chunks) {
        return chunks.stream()
                .map(Chunk::getContent)
                .collect(Collectors.joining("\n\n"));
    }
    
    private String buildPrompt(String query, String context) {
        return String.format("""
                You are a helpful AI assistant. Answer the question based on the provided context.
                If the context doesn't contain enough information, say so.
                
                Context:
                %s
                
                Question: %s
                
                Answer:""", context, query);
    }
    
    private List<String> extractSources(List<Chunk> chunks) {
        return chunks.stream()
                .map(chunk -> {
                    Map<String, Object> metadata = chunk.getMetadata();
                    if (metadata != null && metadata.containsKey("source")) {
                        return metadata.get("source").toString();
                    }
                    return "Unknown source";
                })
                .distinct()
                .collect(Collectors.toList());
    }
    
    public static class RagResponse {
        private String query;
        private String answer;
        private String context;
        private List<String> sources;
        private Integer retrievedChunks;
        
        public static RagResponseBuilder builder() {
            return new RagResponseBuilder();
        }
        
        public static class RagResponseBuilder {
            private String query;
            private String answer;
            private String context;
            private List<String> sources;
            private Integer retrievedChunks;
            
            public RagResponseBuilder query(String query) {
                this.query = query;
                return this;
            }
            
            public RagResponseBuilder answer(String answer) {
                this.answer = answer;
                return this;
            }
            
            public RagResponseBuilder context(String context) {
                this.context = context;
                return this;
            }
            
            public RagResponseBuilder sources(List<String> sources) {
                this.sources = sources;
                return this;
            }
            
            public RagResponseBuilder retrievedChunks(Integer retrievedChunks) {
                this.retrievedChunks = retrievedChunks;
                return this;
            }
            
            public RagResponse build() {
                RagResponse response = new RagResponse();
                response.query = this.query;
                response.answer = this.answer;
                response.context = this.context;
                response.sources = this.sources;
                response.retrievedChunks = this.retrievedChunks;
                return response;
            }
        }
        
        public String getQuery() { return query; }
        public String getAnswer() { return answer; }
        public String getContext() { return context; }
        public List<String> getSources() { return sources; }
        public Integer getRetrievedChunks() { return retrievedChunks; }
    }
}