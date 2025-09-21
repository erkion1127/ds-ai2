package com.dsai.rag.core.graph;

import com.dsai.rag.core.service.RagOrchestrator;
import com.dsai.rag.model.ChatRequest;
import com.dsai.rag.model.QueryRequest;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.service.AiServices;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Function;

@Slf4j
@Component
public class ChatWorkflow {

    private final RagOrchestrator ragOrchestrator;
    private final ChatLanguageModel chatModel;
    private final Map<String, WorkflowState> stateStore = new HashMap<>();

    public ChatWorkflow(RagOrchestrator ragOrchestrator,
                       @Value("${ollama.base-url}") String ollamaUrl,
                       @Value("${ollama.chat-model}") String modelName,
                       @Value("${ollama.timeout:120}") int timeout) {
        this.ragOrchestrator = ragOrchestrator;
        
        this.chatModel = OllamaChatModel.builder()
                .baseUrl(ollamaUrl)
                .modelName(modelName)
                .timeout(java.time.Duration.ofSeconds(timeout))
                .temperature(0.7)
                .build();
                
        log.info("Initialized ChatWorkflow with model: {} at {}", modelName, ollamaUrl);
    }

    @Data
    @Builder
    public static class WorkflowState {
        private String sessionId;
        private String currentStep;
        private List<ChatMessage> messages;
        private Map<String, Object> context;
        private boolean useRag;
        private String lastResponse;
    }

    public enum WorkflowStep {
        START,
        ANALYZE_INTENT,
        RETRIEVE_CONTEXT,
        GENERATE_RESPONSE,
        VALIDATE_RESPONSE,
        END
    }

    public String processChat(ChatRequest request) {
        String sessionId = request.getSessionId();
        WorkflowState state = stateStore.computeIfAbsent(sessionId, k -> 
            WorkflowState.builder()
                .sessionId(sessionId)
                .currentStep(WorkflowStep.START.name())
                .messages(new ArrayList<>())
                .context(new HashMap<>())
                .useRag(request.isUseRag())
                .build()
        );

        // Add user message
        state.getMessages().add(UserMessage.from(request.getMessage()));
        
        // Execute workflow steps
        state = executeStep(WorkflowStep.START, state);
        state = executeStep(WorkflowStep.ANALYZE_INTENT, state);
        
        if (state.isUseRag()) {
            state = executeStep(WorkflowStep.RETRIEVE_CONTEXT, state);
        }
        
        state = executeStep(WorkflowStep.GENERATE_RESPONSE, state);
        state = executeStep(WorkflowStep.VALIDATE_RESPONSE, state);
        state = executeStep(WorkflowStep.END, state);
        
        return state.getLastResponse();
    }

    private WorkflowState executeStep(WorkflowStep step, WorkflowState state) {
        log.debug("Executing step: {} for session: {}", step, state.getSessionId());
        
        switch (step) {
            case START:
                state.setCurrentStep(WorkflowStep.ANALYZE_INTENT.name());
                break;
                
            case ANALYZE_INTENT:
                analyzeIntent(state);
                state.setCurrentStep(WorkflowStep.RETRIEVE_CONTEXT.name());
                break;
                
            case RETRIEVE_CONTEXT:
                if (state.isUseRag()) {
                    retrieveContext(state);
                }
                state.setCurrentStep(WorkflowStep.GENERATE_RESPONSE.name());
                break;
                
            case GENERATE_RESPONSE:
                generateResponse(state);
                state.setCurrentStep(WorkflowStep.VALIDATE_RESPONSE.name());
                break;
                
            case VALIDATE_RESPONSE:
                validateResponse(state);
                state.setCurrentStep(WorkflowStep.END.name());
                break;
                
            case END:
                // Add AI response to message history
                state.getMessages().add(AiMessage.from(state.getLastResponse()));
                break;
        }
        
        return state;
    }

    private void analyzeIntent(WorkflowState state) {
        // Analyze user intent using LLM
        String lastUserMessage = getLastUserMessage(state);
        
        String intentPrompt = String.format(
            "다음 사용자 메시지의 의도를 분석하세요. 정보 검색이 필요한지, 일반 대화인지 판단하세요:\n%s\n\n" +
            "응답 형식: [SEARCH_NEEDED] 또는 [GENERAL_CHAT]",
            lastUserMessage
        );
        
        String intentAnalysis = chatModel.generate(intentPrompt);
        
        if (intentAnalysis.contains("SEARCH_NEEDED")) {
            state.getContext().put("needsSearch", true);
        } else {
            state.getContext().put("needsSearch", false);
        }
        
        log.debug("Intent analysis result: {}", state.getContext().get("needsSearch"));
    }

    private void retrieveContext(WorkflowState state) {
        String lastUserMessage = getLastUserMessage(state);
        
        try {
            QueryRequest queryRequest = new QueryRequest();
            queryRequest.setQuery(lastUserMessage);
            queryRequest.setTopK(5);
            
            var ragResponse = ragOrchestrator.query(queryRequest);
            state.getContext().put("retrievedContext", ragResponse.getContext());
            state.getContext().put("sources", ragResponse.getSources());
            
            log.debug("Retrieved {} chunks for context", ragResponse.getRetrievedChunks());
        } catch (Exception e) {
            log.warn("Failed to retrieve context: {}", e.getMessage());
            state.getContext().put("retrievedContext", "");
        }
    }

    private void generateResponse(WorkflowState state) {
        String lastUserMessage = getLastUserMessage(state);
        String context = (String) state.getContext().getOrDefault("retrievedContext", "");
        
        String prompt;
        if (!context.isEmpty()) {
            prompt = String.format(
                "당신은 도움이 되는 AI 어시스턴트입니다. 다음 컨텍스트를 참고하여 질문에 답변하세요.\n\n" +
                "컨텍스트:\n%s\n\n" +
                "질문: %s\n\n" +
                "답변:",
                context, lastUserMessage
            );
        } else {
            prompt = String.format(
                "당신은 도움이 되는 AI 어시스턴트입니다. 다음 질문에 친절하고 정확하게 답변하세요.\n\n" +
                "질문: %s\n\n" +
                "답변:",
                lastUserMessage
            );
        }
        
        String response = chatModel.generate(prompt);
        state.setLastResponse(response);
        
        log.debug("Generated response of length: {}", response.length());
    }

    private void validateResponse(WorkflowState state) {
        // Validate response quality
        String response = state.getLastResponse();
        
        if (response == null || response.trim().isEmpty()) {
            state.setLastResponse("죄송합니다. 응답을 생성할 수 없습니다. 다시 시도해주세요.");
            return;
        }
        
        // Check for hallucination or off-topic responses
        String validationPrompt = String.format(
            "다음 응답이 적절한지 평가하세요. 부적절하거나 오류가 있으면 'INVALID', 적절하면 'VALID'를 응답하세요.\n\n" +
            "응답: %s\n\n" +
            "평가:",
            response
        );
        
        try {
            String validation = chatModel.generate(validationPrompt);
            if (validation.contains("INVALID")) {
                log.warn("Response validation failed, regenerating...");
                generateResponse(state); // Regenerate response
            }
        } catch (Exception e) {
            log.error("Failed to validate response: {}", e.getMessage());
        }
    }

    private String getLastUserMessage(WorkflowState state) {
        List<ChatMessage> messages = state.getMessages();
        for (int i = messages.size() - 1; i >= 0; i--) {
            if (messages.get(i) instanceof UserMessage) {
                return ((UserMessage) messages.get(i)).singleText();
            }
        }
        return "";
    }

    public void clearSession(String sessionId) {
        stateStore.remove(sessionId);
        log.info("Cleared workflow state for session: {}", sessionId);
    }

    public List<String> getActiveSessions() {
        return new ArrayList<>(stateStore.keySet());
    }
}