package com.dsai.rag.model;

import java.time.LocalDateTime;
import java.util.UUID;

public class ChatMessage {
    private String id;
    private String sessionId;
    private String role; // "user" or "assistant"
    private String content;
    private LocalDateTime timestamp;
    
    public ChatMessage() {
        this.id = UUID.randomUUID().toString();
        this.timestamp = LocalDateTime.now();
    }
    
    public ChatMessage(String sessionId, String role, String content) {
        this();
        this.sessionId = sessionId;
        this.role = role;
        this.content = content;
    }
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getSessionId() {
        return sessionId;
    }
    
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
    
    public String getRole() {
        return role;
    }
    
    public void setRole(String role) {
        this.role = role;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}