package com.dsai.rag.api.service;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ChatMessage;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 대화 컨텍스트를 관리하는 서비스
 * 세션별로 대화 이력을 저장하고 컨텍스트를 제공합니다
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ConversationMemoryService {
    
    // 세션별 대화 이력 저장
    private final Map<String, ConversationSession> sessions = new ConcurrentHashMap<>();
    
    // 기본 컨텍스트 윈도우 크기
    private static final int DEFAULT_CONTEXT_WINDOW = 10;
    
    // 세션 타임아웃 (30분)
    private static final long SESSION_TIMEOUT_MINUTES = 30;
    
    /**
     * 세션에 메시지 추가
     */
    public void addMessage(String sessionId, String role, String content, Map<String, Object> metadata) {
        ConversationSession session = sessions.computeIfAbsent(sessionId, 
            k -> new ConversationSession(sessionId));
        
        ConversationMessage message = new ConversationMessage();
        message.setRole(role);
        message.setContent(content);
        message.setMetadata(metadata);
        message.setTimestamp(LocalDateTime.now());
        
        session.addMessage(message);
        log.debug("Added message to session {}: role={}, length={}", 
            sessionId, role, content.length());
    }
    
    /**
     * 사용자 메시지 추가
     */
    public void addUserMessage(String sessionId, String content) {
        addMessage(sessionId, "user", content, null);
    }
    
    /**
     * AI 응답 추가
     */
    public void addAssistantMessage(String sessionId, String content, Map<String, Object> metadata) {
        addMessage(sessionId, "assistant", content, metadata);
    }
    
    /**
     * 시스템 메시지 추가
     */
    public void addSystemMessage(String sessionId, String content) {
        addMessage(sessionId, "system", content, null);
    }
    
    /**
     * 세션의 최근 대화 컨텍스트 가져오기
     */
    public List<ConversationMessage> getContext(String sessionId, int windowSize) {
        ConversationSession session = sessions.get(sessionId);
        if (session == null) {
            return new ArrayList<>();
        }
        
        List<ConversationMessage> messages = session.getMessages();
        int startIndex = Math.max(0, messages.size() - windowSize);
        return messages.subList(startIndex, messages.size());
    }
    
    /**
     * 기본 윈도우 크기로 컨텍스트 가져오기
     */
    public List<ConversationMessage> getContext(String sessionId) {
        return getContext(sessionId, DEFAULT_CONTEXT_WINDOW);
    }
    
    /**
     * LangChain4j 형식의 메시지 리스트로 변환
     */
    public List<ChatMessage> getContextAsLangChainMessages(String sessionId) {
        return getContext(sessionId).stream()
            .map(msg -> {
                switch (msg.getRole()) {
                    case "user":
                        return (ChatMessage) UserMessage.from(msg.getContent());
                    case "assistant":
                        return (ChatMessage) AiMessage.from(msg.getContent());
                    case "system":
                        return (ChatMessage) SystemMessage.from(msg.getContent());
                    default:
                        return (ChatMessage) UserMessage.from(msg.getContent());
                }
            })
            .collect(Collectors.toList());
    }
    
    /**
     * 대화 요약 생성 (긴 대화를 압축)
     */
    public String summarizeContext(String sessionId) {
        List<ConversationMessage> messages = getContext(sessionId, 20);
        if (messages.isEmpty()) {
            return "";
        }
        
        StringBuilder summary = new StringBuilder();
        summary.append("대화 요약:\n");
        
        // 주요 의도들 추출
        Set<String> intents = new HashSet<>();
        List<String> keyActions = new ArrayList<>();
        
        for (ConversationMessage msg : messages) {
            if (msg.getMetadata() != null) {
                Object intent = msg.getMetadata().get("intent");
                if (intent != null) {
                    intents.add(intent.toString());
                }
                Object action = msg.getMetadata().get("action");
                if (action != null) {
                    keyActions.add(action.toString());
                }
            }
        }
        
        if (!intents.isEmpty()) {
            summary.append("논의된 주제: ").append(String.join(", ", intents)).append("\n");
        }
        if (!keyActions.isEmpty()) {
            summary.append("수행된 작업: ").append(String.join(", ", keyActions)).append("\n");
        }
        
        return summary.toString();
    }
    
    /**
     * 세션 컨텍스트 초기화
     */
    public void clearSession(String sessionId) {
        sessions.remove(sessionId);
        log.info("Cleared session context: {}", sessionId);
    }
    
    /**
     * 오래된 세션 정리
     */
    public void cleanupOldSessions() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(SESSION_TIMEOUT_MINUTES);
        
        sessions.entrySet().removeIf(entry -> {
            ConversationSession session = entry.getValue();
            if (session.getLastActivity().isBefore(cutoff)) {
                log.info("Removing inactive session: {}", entry.getKey());
                return true;
            }
            return false;
        });
    }
    
    /**
     * 세션 정보 조회
     */
    public Map<String, Object> getSessionInfo(String sessionId) {
        ConversationSession session = sessions.get(sessionId);
        if (session == null) {
            return Collections.emptyMap();
        }
        
        Map<String, Object> info = new HashMap<>();
        info.put("sessionId", sessionId);
        info.put("messageCount", session.getMessages().size());
        info.put("createdAt", session.getCreatedAt());
        info.put("lastActivity", session.getLastActivity());
        info.put("summary", summarizeContext(sessionId));
        
        return info;
    }
    
    /**
     * 대화 세션 클래스
     */
    @Data
    public static class ConversationSession {
        private final String sessionId;
        private final List<ConversationMessage> messages = new ArrayList<>();
        private final LocalDateTime createdAt = LocalDateTime.now();
        private LocalDateTime lastActivity = LocalDateTime.now();
        
        public ConversationSession(String sessionId) {
            this.sessionId = sessionId;
        }
        
        public void addMessage(ConversationMessage message) {
            messages.add(message);
            lastActivity = LocalDateTime.now();
        }
    }
    
    /**
     * 대화 메시지 클래스
     */
    @Data
    public static class ConversationMessage {
        private String role;          // user, assistant, system
        private String content;        // 메시지 내용
        private LocalDateTime timestamp;
        private Map<String, Object> metadata; // 의도, 엔티티 등 추가 정보
    }
}