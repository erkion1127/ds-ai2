package com.dsai.rag.model;

public class ChatRequest {
    private String sessionId;
    private String message;
    private boolean useRag = true;
    private int maxTokens = 1000;
    
    public ChatRequest() {}
    
    public ChatRequest(String sessionId, String message) {
        this.sessionId = sessionId;
        this.message = message;
    }
    
    public String getSessionId() {
        return sessionId;
    }
    
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public boolean isUseRag() {
        return useRag;
    }
    
    public void setUseRag(boolean useRag) {
        this.useRag = useRag;
    }
    
    public int getMaxTokens() {
        return maxTokens;
    }
    
    public void setMaxTokens(int maxTokens) {
        this.maxTokens = maxTokens;
    }
}