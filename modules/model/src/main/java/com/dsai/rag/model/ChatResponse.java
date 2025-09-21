package com.dsai.rag.model;

import java.util.List;

public class ChatResponse {
    private String sessionId;
    private String response;
    private List<String> sources;
    private long responseTimeMs;
    
    public ChatResponse() {}
    
    public ChatResponse(String sessionId, String response) {
        this.sessionId = sessionId;
        this.response = response;
    }
    
    public String getSessionId() {
        return sessionId;
    }
    
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
    
    public String getResponse() {
        return response;
    }
    
    public void setResponse(String response) {
        this.response = response;
    }
    
    public List<String> getSources() {
        return sources;
    }
    
    public void setSources(List<String> sources) {
        this.sources = sources;
    }
    
    public long getResponseTimeMs() {
        return responseTimeMs;
    }
    
    public void setResponseTimeMs(long responseTimeMs) {
        this.responseTimeMs = responseTimeMs;
    }
}