package com.dsai.rag.core.service;

import com.dsai.rag.core.graph.ChatWorkflow;
import com.dsai.rag.model.ChatMessage;
import com.dsai.rag.model.ChatRequest;
import com.dsai.rag.model.ChatResponse;
import com.dsai.rag.model.QueryRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ChatService {
    
    private static final Logger logger = LoggerFactory.getLogger(ChatService.class);
    
    private final RagOrchestrator ragOrchestrator;
    private final ChatWorkflow chatWorkflow;
    private ChatLanguageModel chatModel;
    private final Map<String, ChatMemory> sessionMemories = new ConcurrentHashMap<>();
    private final Map<String, List<ChatMessage>> chatHistories = new ConcurrentHashMap<>();
    
    @Value("${ollama.base-url:http://localhost:11434}")
    private String ollamaBaseUrl;
    
    @Value("${ollama.chat-model:llama3.2}")
    private String chatModelName;
    
    @Value("${chat.memory.window-size:10}")
    private int memoryWindowSize;
    
    public ChatService(RagOrchestrator ragOrchestrator,
                      ChatWorkflow chatWorkflow,
                      @Value("${ollama.base-url:http://localhost:11434}") String ollamaBaseUrl,
                      @Value("${ollama.chat-model:llama3.2}") String chatModelName,
                      @Value("${chat.memory.window-size:10}") int memoryWindowSize) {
        this.ragOrchestrator = ragOrchestrator;
        this.chatWorkflow = chatWorkflow;
        this.ollamaBaseUrl = ollamaBaseUrl;
        this.chatModelName = chatModelName;
        this.memoryWindowSize = memoryWindowSize;
        this.chatModel = initializeChatModel();
    }
    
    private ChatLanguageModel initializeChatModel() {
        return OllamaChatModel.builder()
                .baseUrl(ollamaBaseUrl)
                .modelName(chatModelName)
                .temperature(0.7)
                .build();
    }
    
    public ChatResponse chat(ChatRequest request) {
        long startTime = System.currentTimeMillis();
        
        String sessionId = request.getSessionId();
        if (sessionId == null || sessionId.isEmpty()) {
            sessionId = java.util.UUID.randomUUID().toString();
            request.setSessionId(sessionId);
        }
        
        // Store chat history
        List<ChatMessage> history = chatHistories.computeIfAbsent(sessionId, 
            k -> new ArrayList<>());
        history.add(new ChatMessage(sessionId, "user", request.getMessage()));
        
        // Use ChatWorkflow for processing
        String response;
        List<String> sources = null;
        
        try {
            // Process through workflow
            response = chatWorkflow.processChat(request);
            logger.info("Chat processed through workflow for session: {}", sessionId);
        } catch (Exception e) {
            logger.error("Workflow processing failed, falling back to direct chat", e);
            
            // Fallback to direct chat
            ChatMemory memory = sessionMemories.computeIfAbsent(sessionId, 
                k -> MessageWindowChatMemory.withMaxMessages(memoryWindowSize));
            memory.add(UserMessage.from(request.getMessage()));
            response = chatModel.generate(memory.messages()).content().text();
            memory.add(AiMessage.from(response));
        }
        
        
        history.add(new ChatMessage(sessionId, "assistant", response));
        
        ChatResponse chatResponse = new ChatResponse(sessionId, response);
        chatResponse.setSources(sources);
        chatResponse.setResponseTimeMs(System.currentTimeMillis() - startTime);
        
        logger.info("Chat response generated for session: {} in {}ms", 
                   sessionId, chatResponse.getResponseTimeMs());
        
        return chatResponse;
    }
    
    public List<ChatMessage> getChatHistory(String sessionId) {
        return chatHistories.getOrDefault(sessionId, new ArrayList<>());
    }
    
    public void clearSession(String sessionId) {
        sessionMemories.remove(sessionId);
        chatHistories.remove(sessionId);
        chatWorkflow.clearSession(sessionId);
        logger.info("Cleared chat session: {}", sessionId);
    }
    
    public List<String> getActiveSessions() {
        return new ArrayList<>(sessionMemories.keySet());
    }
}