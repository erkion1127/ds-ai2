package com.dsai.rag.api.controller;

import com.dsai.rag.common.dto.BaseResponse;
import com.dsai.rag.core.service.ChatService;
import com.dsai.rag.model.ChatMessage;
import com.dsai.rag.model.ChatRequest;
import com.dsai.rag.model.ChatResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/chat")
@Tag(name = "Chat", description = "AI 채팅 API")
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.DELETE, RequestMethod.OPTIONS})
public class ChatController {
    
    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);
    
    private final ChatService chatService;
    
    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }
    
    @PostMapping
    @Operation(summary = "AI와 대화하기", description = "AI와 대화를 시작하거나 계속합니다")
    public ResponseEntity<BaseResponse<ChatResponse>> chat(@RequestBody ChatRequest request) {
        try {
            logger.info("Chat request received - sessionId: {}, message: {}", 
                       request.getSessionId(), request.getMessage());
            
            ChatResponse response = chatService.chat(request);
            
            return ResponseEntity.ok(BaseResponse.success(response));
        } catch (Exception e) {
            logger.error("Chat error", e);
            return ResponseEntity.internalServerError()
                    .body(BaseResponse.error("CHAT_ERROR", "채팅 처리 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }
    
    @GetMapping("/history/{sessionId}")
    @Operation(summary = "대화 기록 조회", description = "특정 세션의 대화 기록을 조회합니다")
    public ResponseEntity<BaseResponse<List<ChatMessage>>> getChatHistory(
            @PathVariable String sessionId) {
        try {
            logger.info("Fetching chat history for session: {}", sessionId);
            
            List<ChatMessage> history = chatService.getChatHistory(sessionId);
            
            return ResponseEntity.ok(BaseResponse.success(history));
        } catch (Exception e) {
            logger.error("Error fetching chat history", e);
            return ResponseEntity.internalServerError()
                    .body(BaseResponse.error("HISTORY_ERROR", "대화 기록 조회 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }
    
    @DeleteMapping("/session/{sessionId}")
    @Operation(summary = "세션 종료", description = "대화 세션을 종료하고 메모리를 정리합니다")
    public ResponseEntity<BaseResponse<String>> clearSession(@PathVariable String sessionId) {
        try {
            logger.info("Clearing session: {}", sessionId);
            
            chatService.clearSession(sessionId);
            
            return ResponseEntity.ok(BaseResponse.success("세션이 성공적으로 종료되었습니다"));
        } catch (Exception e) {
            logger.error("Error clearing session", e);
            return ResponseEntity.internalServerError()
                    .body(BaseResponse.error("SESSION_CLEAR_ERROR", "세션 종료 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }
    
    @GetMapping("/sessions")
    @Operation(summary = "활성 세션 목록", description = "현재 활성화된 채팅 세션 목록을 조회합니다")
    public ResponseEntity<BaseResponse<List<String>>> getActiveSessions() {
        try {
            List<String> sessions = chatService.getActiveSessions();
            
            logger.info("Active sessions count: {}", sessions.size());
            
            return ResponseEntity.ok(BaseResponse.success(sessions));
        } catch (Exception e) {
            logger.error("Error fetching active sessions", e);
            return ResponseEntity.internalServerError()
                    .body(BaseResponse.error("SESSION_LIST_ERROR", "활성 세션 조회 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }
    
    @PostMapping("/new")
    @Operation(summary = "새 대화 시작", description = "새로운 세션 ID로 대화를 시작합니다")
    public ResponseEntity<BaseResponse<String>> startNewChat() {
        try {
            String sessionId = java.util.UUID.randomUUID().toString();
            
            logger.info("New chat session created: {}", sessionId);
            
            return ResponseEntity.ok(BaseResponse.success(sessionId));
        } catch (Exception e) {
            logger.error("Error creating new session", e);
            return ResponseEntity.internalServerError()
                    .body(BaseResponse.error("SESSION_CREATE_ERROR", "새 세션 생성 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }
}