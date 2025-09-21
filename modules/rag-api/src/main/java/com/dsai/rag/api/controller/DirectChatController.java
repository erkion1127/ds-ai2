package com.dsai.rag.api.controller;

import com.dsai.rag.common.dto.BaseResponse;
import com.dsai.rag.core.service.DirectChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/direct-chat")
@Tag(name = "Direct Chat", description = "Ollama 직접 채팅 API")
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.OPTIONS})
public class DirectChatController {
    
    private static final Logger logger = LoggerFactory.getLogger(DirectChatController.class);
    
    private final DirectChatService directChatService;
    
    public DirectChatController(DirectChatService directChatService) {
        this.directChatService = directChatService;
    }
    
    @PostMapping
    @Operation(summary = "Ollama 직접 질의", description = "RAG나 메모리 없이 Ollama 모델에 직접 질의합니다")
    public ResponseEntity<BaseResponse<Map<String, Object>>> directChat(@RequestBody Map<String, String> request) {
        try {
            String message = request.get("message");
            String model = request.get("model");
            
            if (message == null || message.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(BaseResponse.error("INVALID_MESSAGE", "메시지가 비어있습니다"));
            }
            
            logger.info("Direct chat request - message: {}, model: {}", message, model);
            
            Map<String, Object> response = directChatService.directChat(message, model);
            
            return ResponseEntity.ok(BaseResponse.success(response));
        } catch (Exception e) {
            logger.error("Direct chat error", e);
            return ResponseEntity.internalServerError()
                    .body(BaseResponse.error("DIRECT_CHAT_ERROR", "직접 채팅 처리 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }
    
    @GetMapping("/models")
    @Operation(summary = "사용 가능한 모델 목록", description = "Ollama에서 사용 가능한 모델 목록을 조회합니다")
    public ResponseEntity<BaseResponse<Map<String, Object>>> getAvailableModels() {
        try {
            Map<String, Object> models = directChatService.getAvailableModels();
            return ResponseEntity.ok(BaseResponse.success(models));
        } catch (Exception e) {
            logger.error("Error fetching models", e);
            return ResponseEntity.internalServerError()
                    .body(BaseResponse.error("MODEL_LIST_ERROR", "모델 목록 조회 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }
}