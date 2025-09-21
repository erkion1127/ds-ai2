package com.dsai.rag.core.service;

import dev.langchain4j.model.ollama.OllamaChatModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DirectChatService {
    
    private static final Logger logger = LoggerFactory.getLogger(DirectChatService.class);
    
    @Value("${ollama.base-url:http://localhost:11434}")
    private String ollamaBaseUrl;
    
    @Value("${ollama.chat-model:llama3.2-vision:11b}")
    private String defaultChatModel;
    
    @Value("${ollama.timeout:120}")
    private int timeout;
    
    public Map<String, Object> directChat(String message, String model) {
        long startTime = System.currentTimeMillis();
        
        // Use provided model or default
        String modelToUse = (model != null && !model.trim().isEmpty()) ? model : defaultChatModel;
        
        logger.info("Direct chat with Ollama - model: {}, baseUrl: {}", modelToUse, ollamaBaseUrl);
        
        try {
            // Build Ollama chat model
            OllamaChatModel chatModel = OllamaChatModel.builder()
                    .baseUrl(ollamaBaseUrl)
                    .modelName(modelToUse)
                    .temperature(0.7)
                    .timeout(java.time.Duration.ofSeconds(timeout))
                    .build();
            
            // Generate response
            String response = chatModel.generate(message);
            
            long responseTime = System.currentTimeMillis() - startTime;
            
            Map<String, Object> result = new HashMap<>();
            result.put("response", response);
            result.put("model", modelToUse);
            result.put("responseTimeMs", responseTime);
            result.put("baseUrl", ollamaBaseUrl);
            
            logger.info("Direct chat completed - model: {}, responseTime: {}ms", modelToUse, responseTime);
            
            return result;
        } catch (Exception e) {
            logger.error("Error in direct chat with model: " + modelToUse, e);
            throw new RuntimeException("Direct chat failed: " + e.getMessage(), e);
        }
    }
    
    public Map<String, Object> getAvailableModels() {
        Map<String, Object> result = new HashMap<>();
        result.put("baseUrl", ollamaBaseUrl);
        result.put("defaultModel", defaultChatModel);
        
        // Since we can't use RestTemplate, we'll provide a static list of commonly used models
        // In production, you might want to use OkHttp or another HTTP client
        List<Map<String, String>> models = new ArrayList<>();
        
        Map<String, String> model1 = new HashMap<>();
        model1.put("name", "llama3.2-vision:11b");
        model1.put("size", "11B");
        models.add(model1);
        
        Map<String, String> model2 = new HashMap<>();
        model2.put("name", "llama3.1:8b-instruct-q4_K_M");
        model2.put("size", "8B");
        models.add(model2);
        
        Map<String, String> model3 = new HashMap<>();
        model3.put("name", "nomic-embed-text");
        model3.put("size", "137M");
        models.add(model3);
        
        result.put("models", models);
        
        return result;
    }
}