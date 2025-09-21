package com.dsai.rag.api.service;

import com.dsai.rag.api.entity.*;
import com.dsai.rag.api.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.output.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.annotation.PostConstruct;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PersonalAssistantService {
    
    private final ScheduleRepository scheduleRepository;
    private final NoteRepository noteRepository;
    private final TodoRepository todoRepository;
    private final ObjectMapper objectMapper;
    private final ConversationMemoryService conversationMemory;
    private final AssistantToolService toolService;
    
    @Value("${ollama.base-url:http://localhost:11434}")
    private String ollamaBaseUrl;
    
    @Value("${ollama.chat-model:llama3.2-vision:11b}")
    private String chatModel;
    
    private ChatLanguageModel chatLanguageModel;
    
    @PostConstruct
    public void init() {
        try {
            log.info("Initializing PersonalAssistantService with Ollama model: {}", chatModel);
            chatLanguageModel = OllamaChatModel.builder()
                .baseUrl(ollamaBaseUrl)
                .modelName(chatModel)
                .temperature(0.3)
                .timeout(java.time.Duration.ofSeconds(300))  // 5분으로 증가
                .build();
            log.info("Successfully initialized Ollama chat model");
        } catch (Exception e) {
            log.warn("Failed to initialize Ollama chat model, will use fallback keyword-based analysis", e);
        }
    }
    
    private static final String INTENT_ANALYSIS_PROMPT = """
        당신은 개인 비서 AI의 의도 분석 모듈입니다.
        사용자 메시지를 분석하여 의도와 엔티티를 추출해주세요.
        
        가능한 의도(INTENT):
        - SCHEDULE_ADD: 일정 추가 (회의, 약속, 미팅, 스케줄 등)
        - SCHEDULE_VIEW: 일정 조회/확인
        - SCHEDULE_UPDATE: 일정 변경/수정
        - NOTE_ADD: 메모 작성/저장
        - NOTE_SEARCH: 메모 검색/조회
        - TODO_ADD: 할 일 추가
        - TODO_COMPLETE: 할 일 완료 처리
        - TODO_VIEW: 할 일 목록 조회
        - CHAT: 일반 대화
        
        반드시 아래 JSON 형식으로만 응답하세요. 다른 설명은 포함하지 마세요:
        {
            "intent": "의도",
            "entities": {
                "title": "제목",
                "date": "날짜",
                "time": "시간",
                "description": "설명",
                "location": "장소",
                "priority": "우선순위",
                "period": "기간",
                "keyword": "키워드"
            },
            "confidence": 0.8
        }
        
        필요없는 엔티티는 생략하세요.
        
        사용자 메시지: "{{userMessage}}"
        """;
    
    public Map<String, Object> processMessage(Long userId, String message) {
        return processMessage(userId, message, UUID.randomUUID().toString());
    }
    
    public Map<String, Object> processMessage(Long userId, String message, String sessionId) {
        try {
            log.info("Processing message from user {} in session {}: {}", userId, sessionId, message);
            
            // 대화 컨텍스트에 사용자 메시지 추가
            conversationMemory.addUserMessage(sessionId, message);
            
            // 대화 컨텍스트 가져오기
            List<ConversationMemoryService.ConversationMessage> context = 
                conversationMemory.getContext(sessionId);
            
            // LangChain4j를 사용한 AI 기반 의도 분석 (컨텍스트 포함)
            IntentAnalysis analysis = analyzeIntentWithContext(message, context);
            
            // AI 분석이 실패하면 키워드 기반으로 폴백
            if (analysis == null || analysis.confidence < 0.5) {
                log.info("AI analysis confidence too low, falling back to keyword-based analysis");
                analysis = analyzeIntentSimple(message);
            }
            
            log.info("Analyzed intent: {}, entities: {}, confidence: {}", 
                analysis.intent, analysis.entities, analysis.confidence);
            
            // Tool Service를 사용할 수 있는 의도인지 확인
            boolean useToolService = shouldUseToolService(analysis.intent);
            
            Object result = null;
            String response = "";
            
            if (useToolService && toolService != null) {
                // Tool Service를 사용하여 처리
                try {
                    List<ChatMessage> contextMessages = conversationMemory.getContextAsLangChainMessages(sessionId);
                    Map<String, Object> toolResult = toolService.executeWithTools(userId, message, contextMessages);
                    
                    if ((boolean) toolResult.getOrDefault("success", false)) {
                        response = (String) toolResult.get("response");
                        result = toolResult.get("toolResults");
                        
                        // 도구 사용 정보를 메타데이터에 추가
                        Map<String, Object> metadata = new HashMap<>();
                        metadata.put("intent", analysis.intent);
                        metadata.put("confidence", analysis.confidence);
                        metadata.put("toolsUsed", toolResult.get("toolsUsed"));
                        conversationMemory.addAssistantMessage(sessionId, response, metadata);
                        
                        Map<String, Object> resultMap = new HashMap<>();
                        resultMap.put("intent", analysis.intent);
                        resultMap.put("response", response);
                        resultMap.put("data", result);
                        resultMap.put("confidence", analysis.confidence);
                        resultMap.put("sessionId", sessionId);
                        resultMap.put("contextSize", conversationMemory.getContext(sessionId).size());
                        resultMap.put("toolsUsed", toolResult.get("toolsUsed"));
                        
                        return resultMap;
                    }
                } catch (Exception toolError) {
                    log.warn("Tool service failed, falling back to direct processing", toolError);
                }
            }
            
            // 기존 방식으로 처리
            switch (analysis.intent) {
                case "SCHEDULE_ADD":
                    result = addSchedule(userId, analysis.entities);
                    Schedule addedSchedule = (Schedule) result;
                    response = String.format("✅ 일정이 추가되었습니다!\n📅 %s\n⏰ %s", 
                        addedSchedule.getTitle(),
                        addedSchedule.getStartTime().format(DateTimeFormatter.ofPattern("MM월 dd일 HH시 mm분")));
                    break;
                    
                case "SCHEDULE_VIEW":
                    result = viewSchedules(userId, analysis.entities);
                    response = formatSchedules((List<Schedule>) result);
                    break;
                    
                case "NOTE_ADD":
                    result = addNote(userId, analysis.entities);
                    response = "메모가 저장되었습니다.";
                    break;
                    
                case "NOTE_SEARCH":
                    result = searchNotes(userId, analysis.entities);
                    response = formatNotes((List<Note>) result);
                    break;
                    
                case "TODO_ADD":
                    result = addTodo(userId, analysis.entities);
                    response = "할 일이 추가되었습니다.";
                    break;
                    
                case "TODO_COMPLETE":
                    result = completeTodo(userId, analysis.entities);
                    response = "할 일을 완료했습니다!";
                    break;
                    
                case "TODO_VIEW":
                    result = viewTodos(userId, analysis.entities);
                    response = formatTodos((List<Todo>) result);
                    break;
                    
                default:
                    // AI를 사용한 자연스러운 응답 생성
                    response = generateNaturalResponse(message, analysis.intent, null);
                    break;
            }
            
            // 응답이 너무 기계적이면 AI로 개선
            if (!analysis.intent.equals("CHAT") && chatLanguageModel != null) {
                response = enhanceResponseWithAI(response, result);
            }
            
            // 응답을 대화 컨텍스트에 추가
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("intent", analysis.intent);
            metadata.put("confidence", analysis.confidence);
            if (result != null) {
                metadata.put("action", result.getClass().getSimpleName());
            }
            conversationMemory.addAssistantMessage(sessionId, response, metadata);
            
            Map<String, Object> resultMap = new HashMap<>();
            resultMap.put("intent", analysis.intent);
            resultMap.put("response", response);
            resultMap.put("data", result);
            resultMap.put("confidence", analysis.confidence);
            resultMap.put("sessionId", sessionId);
            resultMap.put("contextSize", conversationMemory.getContext(sessionId).size());
            
            return resultMap;
            
        } catch (Exception e) {
            log.error("Error processing message", e);
            Map<String, Object> errorMap = new HashMap<>();
            errorMap.put("error", true);
            errorMap.put("response", "죄송합니다. 처리 중 오류가 발생했습니다.");
            return errorMap;
        }
    }
    
    /**
     * Tool Service를 사용해야 하는 의도인지 확인
     */
    private boolean shouldUseToolService(String intent) {
        return intent != null && (
            intent.startsWith("SCHEDULE_") ||
            intent.startsWith("NOTE_") ||
            intent.startsWith("TODO_")
        );
    }
    
    private IntentAnalysis analyzeIntentSimple(String message) {
        IntentAnalysis analysis = new IntentAnalysis();
        String lowerMessage = message.toLowerCase();
        
        // 더 유연한 키워드 기반 의도 파악
        // 일정 관련
        if ((lowerMessage.contains("일정") || lowerMessage.contains("스케줄") || lowerMessage.contains("약속") || lowerMessage.contains("미팅") || lowerMessage.contains("회의")) 
            && (lowerMessage.contains("추가") || lowerMessage.contains("등록") || lowerMessage.contains("잡") || lowerMessage.contains("저장") || lowerMessage.contains("넣"))) {
            analysis.intent = "SCHEDULE_ADD";
            analysis.entities.put("title", extractTitle(message));
            // 오늘/내일/모레 등 시간 정보 추출
            if (lowerMessage.contains("오늘")) {
                analysis.entities.put("startTime", LocalDateTime.now().withHour(9).withMinute(0).toString());
            } else if (lowerMessage.contains("내일")) {
                analysis.entities.put("startTime", LocalDateTime.now().plusDays(1).withHour(9).withMinute(0).toString());
            }
        } 
        // 일정 조회
        else if ((lowerMessage.contains("일정") || lowerMessage.contains("스케줄")) 
            && (lowerMessage.contains("보") || lowerMessage.contains("확인") || lowerMessage.contains("알려") || lowerMessage.contains("뭐"))) {
            analysis.intent = "SCHEDULE_VIEW";
            if (lowerMessage.contains("오늘")) {
                analysis.entities.put("period", "today");
            } else if (lowerMessage.contains("이번주") || lowerMessage.contains("이번 주")) {
                analysis.entities.put("period", "week");
            } else {
                analysis.entities.put("period", "upcoming");
            }
        }
        // 메모 추가
        else if ((lowerMessage.contains("메모") || lowerMessage.contains("노트") || lowerMessage.contains("기록")) 
            && (lowerMessage.contains("작성") || lowerMessage.contains("저장") || lowerMessage.contains("추가") || lowerMessage.contains("적") || lowerMessage.contains("남기"))) {
            analysis.intent = "NOTE_ADD";
            analysis.entities.put("content", extractContent(message));
        }
        // 메모 검색
        else if ((lowerMessage.contains("메모") || lowerMessage.contains("노트")) 
            && (lowerMessage.contains("검색") || lowerMessage.contains("찾") || lowerMessage.contains("조회"))) {
            analysis.intent = "NOTE_SEARCH";
            analysis.entities.put("keyword", extractKeyword(message));
        }
        // TODO 추가
        else if ((lowerMessage.contains("todo") || lowerMessage.contains("할일") || lowerMessage.contains("할 일") || lowerMessage.contains("해야 할") || lowerMessage.contains("task")) 
            && (lowerMessage.contains("추가") || lowerMessage.contains("등록") || lowerMessage.contains("만들") || lowerMessage.contains("저장"))) {
            analysis.intent = "TODO_ADD";
            analysis.entities.put("title", extractTitle(message));
        }
        // TODO 완료
        else if ((lowerMessage.contains("todo") || lowerMessage.contains("할일") || lowerMessage.contains("할 일")) 
            && (lowerMessage.contains("완료") || lowerMessage.contains("끝") || lowerMessage.contains("done"))) {
            analysis.intent = "TODO_COMPLETE";
        }
        // TODO 조회
        else if (lowerMessage.contains("todo") || lowerMessage.contains("할일") || lowerMessage.contains("할 일") || lowerMessage.contains("해야")) {
            analysis.intent = "TODO_VIEW";
        }
        // 단순 일정 추가 패턴 (예: "오늘 3시 회의")
        else if (lowerMessage.matches(".*\\d+시.*") || lowerMessage.contains("오늘") || lowerMessage.contains("내일")) {
            if (!lowerMessage.contains("?") && !lowerMessage.contains("뭐") && !lowerMessage.contains("알려")) {
                analysis.intent = "SCHEDULE_ADD";
                analysis.entities.put("title", message);
                if (lowerMessage.contains("오늘")) {
                    analysis.entities.put("startTime", LocalDateTime.now().withHour(extractHour(message)).withMinute(0).toString());
                }
            }
        }
        // 기본 대화
        else {
            analysis.intent = "CHAT";
            analysis.response = "무엇을 도와드릴까요? 일정 추가, 메모 작성, TODO 관리를 할 수 있습니다.\n\n예시:\n- '오늘 3시 회의 일정 추가'\n- '내일 일정 확인'\n- '프로젝트 아이디어 메모 저장'\n- '보고서 작성 할일 추가'";
        }
        
        log.info("Intent analysis result - Intent: {}, Entities: {}", analysis.intent, analysis.entities);
        return analysis;
    }
    
    private int extractHour(String message) {
        Pattern pattern = Pattern.compile("(\\d+)시");
        Matcher matcher = pattern.matcher(message);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        return 9; // 기본값
    }
    
    private String extractTitle(String message) {
        // 따옴표나 특정 패턴에서 제목 추출
        if (message.contains("\"")) {
            int start = message.indexOf("\"") + 1;
            int end = message.lastIndexOf("\"");
            if (end > start) {
                return message.substring(start, end);
            }
        }
        // 간단히 일부 텍스트 반환
        return message.length() > 20 ? message.substring(0, 20) : message;
    }
    
    private String extractContent(String message) {
        return extractTitle(message);
    }
    
    private String extractKeyword(String message) {
        return extractTitle(message);
    }
    
    private Schedule addSchedule(Long userId, Map<String, Object> entities) {
        Schedule schedule = new Schedule();
        schedule.setUserId(userId);
        
        String title = (String) entities.get("title");
        if (title == null || title.trim().isEmpty()) {
            title = "새 일정";
        }
        schedule.setTitle(title);
        schedule.setDescription((String) entities.get("description"));
        
        String startTimeStr = (String) entities.get("startTime");
        if (startTimeStr != null) {
            try {
                schedule.setStartTime(LocalDateTime.parse(startTimeStr));
            } catch (Exception e) {
                log.warn("Failed to parse startTime: {}, using default", startTimeStr);
                schedule.setStartTime(LocalDateTime.now().plusHours(1));
            }
        } else {
            schedule.setStartTime(LocalDateTime.now().plusHours(1));
        }
        
        String endTimeStr = (String) entities.get("endTime");
        if (endTimeStr != null) {
            try {
                schedule.setEndTime(LocalDateTime.parse(endTimeStr));
            } catch (Exception e) {
                log.warn("Failed to parse endTime: {}", endTimeStr);
                schedule.setEndTime(schedule.getStartTime().plusHours(1));
            }
        } else {
            // 기본적으로 1시간 후로 설정
            schedule.setEndTime(schedule.getStartTime().plusHours(1));
        }
        
        schedule.setLocation((String) entities.get("location"));
        schedule.setStatus("scheduled");
        
        Schedule saved = scheduleRepository.save(schedule);
        log.info("Schedule saved: id={}, title={}, startTime={}", saved.getId(), saved.getTitle(), saved.getStartTime());
        return saved;
    }
    
    private List<Schedule> viewSchedules(Long userId, Map<String, Object> entities) {
        String period = (String) entities.getOrDefault("period", "upcoming");
        
        if ("today".equals(period)) {
            LocalDateTime start = LocalDate.now().atStartOfDay();
            LocalDateTime end = start.plusDays(1);
            return scheduleRepository.findByUserIdAndDateRange(userId, start, end);
        } else if ("week".equals(period)) {
            LocalDateTime start = LocalDate.now().atStartOfDay();
            LocalDateTime end = start.plusWeeks(1);
            return scheduleRepository.findByUserIdAndDateRange(userId, start, end);
        } else {
            return scheduleRepository.findUpcomingSchedules(userId, LocalDateTime.now());
        }
    }
    
    private Note addNote(Long userId, Map<String, Object> entities) {
        Note note = new Note();
        note.setUserId(userId);
        note.setTitle((String) entities.get("title"));
        note.setContent((String) entities.get("content"));
        note.setCategory((String) entities.get("category"));
        
        List<String> tags = (List<String>) entities.get("tags");
        if (tags != null) {
            try {
                note.setTags(objectMapper.writeValueAsString(tags));
            } catch (Exception e) {
                log.error("Error serializing tags", e);
            }
        }
        
        return noteRepository.save(note);
    }
    
    private List<Note> searchNotes(Long userId, Map<String, Object> entities) {
        String keyword = (String) entities.get("keyword");
        String category = (String) entities.get("category");
        
        if (keyword != null) {
            return noteRepository.searchNotes(userId, keyword);
        } else if (category != null) {
            return noteRepository.findByUserIdAndCategoryOrderByCreatedAtDesc(userId, category);
        } else {
            return noteRepository.findByUserIdOrderByCreatedAtDesc(userId);
        }
    }
    
    private Todo addTodo(Long userId, Map<String, Object> entities) {
        Todo todo = new Todo();
        todo.setUserId(userId);
        todo.setTitle((String) entities.get("title"));
        todo.setDescription((String) entities.get("description"));
        
        String dueDateStr = (String) entities.get("dueDate");
        if (dueDateStr != null) {
            todo.setDueDate(LocalDate.parse(dueDateStr));
        }
        
        String priority = (String) entities.get("priority");
        if (priority != null) {
            todo.setPriority(Todo.Priority.valueOf(priority.toUpperCase()));
        }
        
        return todoRepository.save(todo);
    }
    
    private Todo completeTodo(Long userId, Map<String, Object> entities) {
        Long todoId = ((Number) entities.get("id")).longValue();
        Todo todo = todoRepository.findById(todoId)
            .orElseThrow(() -> new RuntimeException("Todo not found"));
        
        if (!todo.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized");
        }
        
        todo.setStatus(Todo.TodoStatus.COMPLETED);
        return todoRepository.save(todo);
    }
    
    private List<Todo> viewTodos(Long userId, Map<String, Object> entities) {
        String status = (String) entities.get("status");
        
        if (status != null) {
            return todoRepository.findByUserIdAndStatusOrderByPriorityDescDueDateAsc(
                userId, Todo.TodoStatus.valueOf(status.toUpperCase())
            );
        } else {
            return todoRepository.findByUserIdOrderByDueDateAsc(userId);
        }
    }
    
    private String formatSchedules(List<Schedule> schedules) {
        if (schedules.isEmpty()) {
            return "예정된 일정이 없습니다.";
        }
        
        StringBuilder sb = new StringBuilder("📅 일정 목록:\n");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM월 dd일 HH:mm");
        
        for (Schedule schedule : schedules) {
            sb.append("\n• ").append(schedule.getTitle());
            sb.append(" (").append(schedule.getStartTime().format(formatter)).append(")");
            if (schedule.getLocation() != null) {
                sb.append(" @ ").append(schedule.getLocation());
            }
        }
        
        return sb.toString();
    }
    
    private String formatNotes(List<Note> notes) {
        if (notes.isEmpty()) {
            return "메모가 없습니다.";
        }
        
        StringBuilder sb = new StringBuilder("📝 메모 목록:\n");
        for (Note note : notes) {
            sb.append("\n• ").append(note.getTitle());
            if (note.getCategory() != null) {
                sb.append(" [").append(note.getCategory()).append("]");
            }
        }
        
        return sb.toString();
    }
    
    private String formatTodos(List<Todo> todos) {
        if (todos.isEmpty()) {
            return "할 일이 없습니다.";
        }
        
        StringBuilder sb = new StringBuilder("✅ TODO 목록:\n");
        for (Todo todo : todos) {
            sb.append("\n");
            sb.append(todo.getStatus() == Todo.TodoStatus.COMPLETED ? "✓ " : "☐ ");
            sb.append(todo.getTitle());
            
            if (todo.getPriority() == Todo.Priority.HIGH) {
                sb.append(" ❗");
            }
            
            if (todo.getDueDate() != null) {
                sb.append(" (").append(todo.getDueDate()).append(")");
            }
        }
        
        return sb.toString();
    }
    
    private IntentAnalysis analyzeIntentWithContext(String message, 
                                                     List<ConversationMemoryService.ConversationMessage> context) {
        try {
            // Lazy initialization of chat model
            if (chatLanguageModel == null) {
                chatLanguageModel = OllamaChatModel.builder()
                    .baseUrl(ollamaBaseUrl)
                    .modelName(chatModel)
                    .temperature(0.3)
                    .timeout(java.time.Duration.ofSeconds(300))  // 5분으로 증가
                    .build();
            }
            
            // 컨텍스트를 포함한 프롬프트 생성
            StringBuilder contextPrompt = new StringBuilder();
            if (context != null && !context.isEmpty()) {
                contextPrompt.append("이전 대화 내용:\n");
                for (ConversationMemoryService.ConversationMessage msg : context) {
                    contextPrompt.append(msg.getRole()).append(": ").append(msg.getContent()).append("\n");
                }
                contextPrompt.append("\n");
            }
            
            // AI에게 의도 분석 요청
            String fullPrompt = contextPrompt.toString() + INTENT_ANALYSIS_PROMPT.replace("{{userMessage}}", message);
            Response<AiMessage> response = chatLanguageModel.generate(UserMessage.from(fullPrompt));
            String aiResponse = response.content().text();
            
            log.debug("AI intent analysis response with context: {}", aiResponse);
            
            // JSON 파싱
            String jsonText = extractJsonFromText(aiResponse);
            if (jsonText == null || jsonText.isEmpty()) {
                log.warn("No valid JSON found in AI response");
                return null;
            }
            
            Map<String, Object> analysisResult;
            try {
                analysisResult = objectMapper.readValue(jsonText, Map.class);
            } catch (Exception jsonError) {
                log.error("Failed to parse JSON from AI response: {}", jsonText, jsonError);
                return null;
            }
            
            IntentAnalysis analysis = new IntentAnalysis();
            analysis.intent = (String) analysisResult.get("intent");
            if (analysis.intent == null || analysis.intent.isEmpty()) {
                log.warn("No intent found in AI response");
                return null;
            }
            
            Object entitiesObj = analysisResult.get("entities");
            if (entitiesObj instanceof Map) {
                analysis.entities = (Map<String, Object>) entitiesObj;
            } else {
                analysis.entities = new HashMap<>();
            }
            
            Object confidenceObj = analysisResult.get("confidence");
            if (confidenceObj instanceof Number) {
                analysis.confidence = ((Number) confidenceObj).doubleValue();
            } else {
                analysis.confidence = 0.7; // Default confidence
            }
            
            // 날짜/시간 정보 정규화
            normalizeDateTime(analysis.entities, message);
            
            log.info("AI Intent Analysis with context successful - Intent: {}, Confidence: {}", 
                analysis.intent, analysis.confidence);
            return analysis;
            
        } catch (Exception e) {
            log.error("Failed to analyze intent with context: {}", e.getMessage());
            return null;
        }
    }
    
    private IntentAnalysis analyzeIntentWithAI(String message) {
        try {
            // Lazy initialization of chat model
            if (chatLanguageModel == null) {
                chatLanguageModel = OllamaChatModel.builder()
                    .baseUrl(ollamaBaseUrl)
                    .modelName(chatModel)
                    .temperature(0.3)
                    .timeout(java.time.Duration.ofSeconds(300))  // 5분으로 증가
                    .build();
            }
            
            // AI에게 의도 분석 요청
            String prompt = INTENT_ANALYSIS_PROMPT.replace("{{userMessage}}", message);
            Response<AiMessage> response = chatLanguageModel.generate(UserMessage.from(prompt));
            String aiResponse = response.content().text();
            
            log.debug("AI intent analysis response: {}", aiResponse);
            
            // JSON 파싱
            String jsonText = extractJsonFromText(aiResponse);
            if (jsonText == null || jsonText.isEmpty()) {
                log.warn("No valid JSON found in AI response");
                return null;
            }
            
            Map<String, Object> analysisResult;
            try {
                analysisResult = objectMapper.readValue(jsonText, Map.class);
            } catch (Exception jsonError) {
                log.error("Failed to parse JSON from AI response: {}", jsonText, jsonError);
                return null;
            }
            
            IntentAnalysis analysis = new IntentAnalysis();
            analysis.intent = (String) analysisResult.get("intent");
            if (analysis.intent == null || analysis.intent.isEmpty()) {
                log.warn("No intent found in AI response");
                return null;
            }
            
            Object entitiesObj = analysisResult.get("entities");
            if (entitiesObj instanceof Map) {
                analysis.entities = (Map<String, Object>) entitiesObj;
            } else {
                analysis.entities = new HashMap<>();
            }
            
            Object confidenceObj = analysisResult.get("confidence");
            if (confidenceObj instanceof Number) {
                analysis.confidence = ((Number) confidenceObj).doubleValue();
            } else {
                analysis.confidence = 0.7; // Default confidence
            }
            
            // 날짜/시간 정보 정규화
            normalizeDateTime(analysis.entities, message);
            
            log.info("AI Intent Analysis successful - Intent: {}, Confidence: {}", analysis.intent, analysis.confidence);
            return analysis;
            
        } catch (Exception e) {
            log.error("Failed to analyze intent with AI: {}", e.getMessage());
            return null;
        }
    }
    
    private String extractJsonFromText(String text) {
        if (text == null || text.isEmpty()) {
            return null;
        }
        
        // Remove any markdown code blocks
        text = text.replaceAll("```json\\s*", "").replaceAll("```\\s*", "");
        
        // Find the first { and last } to extract JSON
        int start = text.indexOf("{");
        int end = -1;
        
        if (start >= 0) {
            // Count brackets to find matching closing bracket
            int bracketCount = 0;
            for (int i = start; i < text.length(); i++) {
                if (text.charAt(i) == '{') {
                    bracketCount++;
                } else if (text.charAt(i) == '}') {
                    bracketCount--;
                    if (bracketCount == 0) {
                        end = i;
                        break;
                    }
                }
            }
        }
        
        if (start >= 0 && end > start) {
            String json = text.substring(start, end + 1);
            // Clean up any common formatting issues
            json = json.replaceAll("\\s+", " ")  // Normalize whitespace
                      .replaceAll(",\\s*}", "}")      // Remove trailing commas
                      .replaceAll(",\\s*]", "]");      // Remove trailing commas in arrays
            return json;
        }
        
        return null;
    }
    
    private void normalizeDateTime(Map<String, Object> entities, String originalMessage) {
        String dateStr = (String) entities.get("date");
        String timeStr = (String) entities.get("time");
        
        LocalDateTime dateTime = LocalDateTime.now();
        
        // 날짜 파싱
        if (dateStr != null) {
            String lowerDate = dateStr.toLowerCase();
            if (lowerDate.contains("오늘") || lowerDate.contains("today")) {
                dateTime = LocalDate.now().atStartOfDay();
            } else if (lowerDate.contains("내일") || lowerDate.contains("tomorrow")) {
                dateTime = LocalDate.now().plusDays(1).atStartOfDay();
            } else if (lowerDate.contains("모레")) {
                dateTime = LocalDate.now().plusDays(2).atStartOfDay();
            } else {
                // YYYY-MM-DD 형식 파싱 시도
                try {
                    dateTime = LocalDate.parse(dateStr).atStartOfDay();
                } catch (Exception e) {
                    // 파싱 실패 시 기본값 유지
                }
            }
        }
        
        // 시간 파싱
        if (timeStr != null) {
            Pattern timePattern = Pattern.compile("(\\d{1,2})[:시](\\d{0,2})?분?");
            Matcher matcher = timePattern.matcher(timeStr);
            if (matcher.find()) {
                int hour = Integer.parseInt(matcher.group(1));
                int minute = matcher.group(2) != null && !matcher.group(2).isEmpty() ? 
                    Integer.parseInt(matcher.group(2)) : 0;
                dateTime = dateTime.withHour(hour).withMinute(minute);
            }
        } else {
            // 시간이 없으면 오전 9시로 기본 설정
            dateTime = dateTime.withHour(9).withMinute(0);
        }
        
        entities.put("startTime", dateTime.toString());
    }
    
    private String generateNaturalResponse(String userMessage, String intent, Object data) {
        try {
            if (chatLanguageModel == null) {
                return "무엇을 도와드릴까요?";
            }
            
            String prompt = String.format("""
                사용자 메시지: "%s"
                
                친절한 개인 비서로서 자연스럽게 응답해주세요.
                너무 길지 않게 1-2문장으로 간결하게 답변하세요.
                """, userMessage);
            
            Response<AiMessage> response = chatLanguageModel.generate(UserMessage.from(prompt));
            return response.content().text();
            
        } catch (Exception e) {
            log.error("Failed to generate natural response", e);
            return "무엇을 도와드릴까요? 일정 추가, 메모 작성, TODO 관리를 할 수 있습니다.";
        }
    }
    
    private String enhanceResponseWithAI(String basicResponse, Object data) {
        try {
            if (chatLanguageModel == null) {
                return basicResponse;
            }
            
            String prompt = String.format("""
                다음 응답을 더 자연스럽고 친근하게 만들어주세요:
                원본: %s
                
                요구사항:
                - 이모지 적절히 사용
                - 친근한 말투
                - 1-2문장으로 간결하게
                """, basicResponse);
            
            Response<AiMessage> response = chatLanguageModel.generate(UserMessage.from(prompt));
            return response.content().text();
            
        } catch (Exception e) {
            log.error("Failed to enhance response with AI", e);
            return basicResponse;
        }
    }
    
    private static class IntentAnalysis {
        String intent = "CHAT";
        Map<String, Object> entities = new HashMap<>();
        String response = "";
        double confidence = 1.0;
    }
}