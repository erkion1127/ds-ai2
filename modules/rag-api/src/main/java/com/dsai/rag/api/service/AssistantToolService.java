package com.dsai.rag.api.service;

import com.dsai.rag.api.entity.*;
import com.dsai.rag.api.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.*;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
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
import java.util.stream.Collectors;

/**
 * AI 어시스턴트가 직접 도구를 실행할 수 있도록 하는 서비스
 * LangChain4j의 Tool 기능을 사용하여 구현
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AssistantToolService {

    private final ScheduleRepository scheduleRepository;
    private final NoteRepository noteRepository;
    private final TodoRepository todoRepository;
    private final ObjectMapper objectMapper;

    @Value("${ollama.base-url:http://localhost:11434}")
    private String ollamaBaseUrl;

    @Value("${ollama.chat-model:llama3.2:3b}")
    private String toolModel;

    private ChatLanguageModel toolChatModel;
    private Map<String, java.util.function.Function<Map<String, Object>, String>> toolExecutors = new HashMap<>();

    @PostConstruct
    public void init() {
        try {
            log.info("Initializing Assistant Tool Service with model: {}", toolModel);

            // Tool-enabled chat model 초기화
            toolChatModel = OllamaChatModel.builder()
                .baseUrl(ollamaBaseUrl)
                .modelName(toolModel)
                .temperature(0.1) // 낮은 temperature로 정확도 향상
                .timeout(java.time.Duration.ofSeconds(60))
                .build();

            // 도구 등록
            registerTools();

            log.info("Successfully initialized Tool Service with {} tools", toolExecutors.size());
        } catch (Exception e) {
            log.error("Failed to initialize Tool Service", e);
        }
    }

    /**
     * 사용 가능한 도구들을 등록
     */
    private void registerTools() {
        // 일정 추가 도구
        registerTool("add_schedule", 
            "Add a new schedule/appointment/meeting",
            Arrays.asList(
                param("title", "Title of the schedule", true),
                param("date", "Date (today/tomorrow/YYYY-MM-DD)", false),
                param("time", "Time (HH:mm or Korean format)", false),
                param("location", "Location of the event", false),
                param("description", "Description of the event", false)
            ),
            this::addScheduleTool
        );

        // 일정 조회 도구
        registerTool("get_schedules",
            "Get list of schedules",
            Arrays.asList(
                param("period", "Period to query (today/week/month/all)", false)
            ),
            this::getSchedulesTool
        );

        // 메모 추가 도구
        registerTool("add_note",
            "Add a new note or memo",
            Arrays.asList(
                param("title", "Title of the note", false),
                param("content", "Content of the note", true),
                param("category", "Category of the note", false)
            ),
            this::addNoteTool
        );

        // 메모 검색 도구
        registerTool("search_notes",
            "Search for notes",
            Arrays.asList(
                param("keyword", "Keyword to search", false),
                param("category", "Category to filter", false)
            ),
            this::searchNotesTool
        );

        // TODO 추가 도구
        registerTool("add_todo",
            "Add a new todo/task",
            Arrays.asList(
                param("title", "Title of the todo", true),
                param("description", "Description of the todo", false),
                param("due_date", "Due date (YYYY-MM-DD)", false),
                param("priority", "Priority (low/medium/high)", false)
            ),
            this::addTodoTool
        );

        // TODO 조회 도구
        registerTool("get_todos",
            "Get list of todos",
            Arrays.asList(
                param("status", "Status filter (pending/completed/all)", false)
            ),
            this::getTodosTool
        );

        // TODO 완료 도구
        registerTool("complete_todo",
            "Mark a todo as completed",
            Arrays.asList(
                param("todo_id", "ID of the todo to complete", false),
                param("title", "Title of the todo to complete (if ID not provided)", false)
            ),
            this::completeTodoTool
        );
    }

    /**
     * 도구 파라미터 생성 헬퍼
     */
    private ToolParameter param(String name, String description, boolean required) {
        return new ToolParameter(name, description, required);
    }

    /**
     * 도구 등록 헬퍼
     */
    private void registerTool(String name, String description, 
                              List<ToolParameter> parameters, 
                              java.util.function.Function<Map<String, Object>, String> executor) {
        toolExecutors.put(name, executor);
        log.debug("Registered tool: {}", name);
    }

    /**
     * AI가 도구를 사용하여 사용자 요청 처리
     */
    public Map<String, Object> executeWithTools(Long userId, String message, 
                                                 List<ChatMessage> conversationHistory) {
        Map<String, Object> result = new HashMap<>();

        try {
            // 도구 사양 생성
            List<ToolSpecification> toolSpecs = createToolSpecifications();

            // 시스템 메시지로 도구 사용 가이드 제공
            SystemMessage systemMessage = SystemMessage.from("""
                You are a helpful personal assistant with access to tools.
                Use the provided tools to help users manage their schedules, notes, and todos.
                Always use tools when the user requests an action.
                Respond in Korean.

                Current datetime: """ + LocalDateTime.now().toString());

            // 대화 이력 구성
            List<ChatMessage> messages = new ArrayList<>();
            messages.add(systemMessage);
            if (conversationHistory != null) {
                messages.addAll(conversationHistory);
            }
            messages.add(UserMessage.from(message));

            // AI에게 도구와 함께 요청
            Response<AiMessage> response = toolChatModel.generate(messages);
            AiMessage aiMessage = response.content();

            // 도구 실행 요청 확인
            if (aiMessage.hasToolExecutionRequests()) {
                List<ToolExecutionRequest> toolRequests = aiMessage.toolExecutionRequests();
                List<ToolExecutionResult> toolResults = new ArrayList<>();

                for (ToolExecutionRequest request : toolRequests) {
                    ToolExecutionResult executionResult = executeToolRequest(userId, request);
                    toolResults.add(executionResult);
                }

                // 도구 실행 결과를 포함하여 최종 응답 생성
                messages.add(aiMessage);
                for (ToolExecutionResult toolResult : toolResults) {
                    messages.add(ToolExecutionResultMessage.from(
                        toolResult.request, toolResult.output
                    ));
                }

                // 최종 응답 생성
                Response<AiMessage> finalResponse = toolChatModel.generate(messages);

                result.put("success", true);
                result.put("response", finalResponse.content().text());
                result.put("toolsUsed", toolResults.stream()
                    .map(r -> r.request.name())
                    .collect(Collectors.toList()));
                result.put("toolResults", toolResults.stream()
                    .map(r -> Map.of(
                        "tool", r.request.name(),
                        "result", r.output
                    ))
                    .collect(Collectors.toList()));
            } else {
                // 도구 사용 없이 일반 응답
                result.put("success", true);
                result.put("response", aiMessage.text());
                result.put("toolsUsed", Collections.emptyList());
            }

        } catch (Exception e) {
            log.error("Error executing with tools", e);
            result.put("success", false);
            result.put("error", e.getMessage());
            result.put("response", "도구 실행 중 오류가 발생했습니다: " + e.getMessage());
        }

        return result;
    }

    /**
     * 도구 실행 요청 처리
     */
    private ToolExecutionResult executeToolRequest(Long userId, ToolExecutionRequest request) {
        String toolName = request.name();
        java.util.function.Function<Map<String, Object>, String> executor = toolExecutors.get(toolName);

        if (executor == null) {
            log.error("Unknown tool requested: {}", toolName);
            return new ToolExecutionResult(request, "Error: Unknown tool " + toolName);
        }

        try {
            // 파라미터에 userId 추가
            Map<String, Object> arguments = new HashMap<>();

            // 요청에서 인자 추출 (LangChain4j 0.35.0 버전에 맞게 수정)
            try {
                // 인자가 JSON 문자열인 경우 파싱
                String argsStr = request.arguments().toString();
                if (argsStr != null && !argsStr.isEmpty()) {
                    // JSON 파싱 시도
                    try {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> parsedArgs = objectMapper.readValue(argsStr, Map.class);
                        arguments.putAll(parsedArgs);
                    } catch (Exception e) {
                        log.warn("Failed to parse arguments as JSON: {}", argsStr, e);
                    }
                }
            } catch (Exception e) {
                log.warn("Error extracting arguments from request", e);
            }

            arguments.put("userId", userId);

            String result = executor.apply(arguments);
            log.info("Tool {} executed successfully", toolName);
            return new ToolExecutionResult(request, result);

        } catch (Exception e) {
            log.error("Error executing tool {}", toolName, e);
            return new ToolExecutionResult(request, "Error: " + e.getMessage());
        }
    }

    /**
     * 도구 사양 생성
     */
    private List<ToolSpecification> createToolSpecifications() {
        List<ToolSpecification> specs = new ArrayList<>();

        // 각 도구에 대한 사양 생성
        // LangChain4j 0.35.0 버전에 맞게 수정
        try {
            // 일정 추가 도구
            ToolSpecification addScheduleSpec = ToolSpecification.builder()
                .name("add_schedule")
                .description("Add a new schedule or appointment")
                .build();
            specs.add(addScheduleSpec);

            // 일정 조회 도구
            ToolSpecification getSchedulesSpec = ToolSpecification.builder()
                .name("get_schedules")
                .description("Get list of schedules")
                .build();
            specs.add(getSchedulesSpec);

            // 메모 추가 도구
            ToolSpecification addNoteSpec = ToolSpecification.builder()
                .name("add_note")
                .description("Add a new note or memo")
                .build();
            specs.add(addNoteSpec);

            // 메모 검색 도구
            ToolSpecification searchNotesSpec = ToolSpecification.builder()
                .name("search_notes")
                .description("Search for notes")
                .build();
            specs.add(searchNotesSpec);

            // TODO 추가 도구
            ToolSpecification addTodoSpec = ToolSpecification.builder()
                .name("add_todo")
                .description("Add a new todo or task")
                .build();
            specs.add(addTodoSpec);

            // TODO 완료 도구
            ToolSpecification completeTodoSpec = ToolSpecification.builder()
                .name("complete_todo")
                .description("Mark a todo as completed")
                .build();
            specs.add(completeTodoSpec);
        } catch (Exception e) {
            log.error("Error creating tool specifications", e);
        }

        return specs;
    }

    // === 도구 구현 메서드들 ===

    private String addScheduleTool(Map<String, Object> params) {
        Long userId = (Long) params.get("userId");

        Schedule schedule = new Schedule();
        schedule.setUserId(userId);
        schedule.setTitle((String) params.get("title"));
        schedule.setDescription((String) params.get("description"));
        schedule.setLocation((String) params.get("location"));

        // 날짜/시간 파싱
        LocalDateTime startTime = parseDateTime(
            (String) params.get("date"),
            (String) params.get("time")
        );
        schedule.setStartTime(startTime);
        schedule.setEndTime(startTime.plusHours(1)); // 기본 1시간
        schedule.setStatus("scheduled");

        Schedule saved = scheduleRepository.save(schedule);

        return String.format("일정 '%s'이(가) %s에 추가되었습니다.",
            saved.getTitle(),
            saved.getStartTime().format(DateTimeFormatter.ofPattern("MM월 dd일 HH:mm"))
        );
    }

    private String getSchedulesTool(Map<String, Object> params) {
        Long userId = (Long) params.get("userId");
        String period = (String) params.getOrDefault("period", "today");

        List<Schedule> schedules;
        LocalDateTime now = LocalDateTime.now();

        switch (period.toLowerCase()) {
            case "today":
                LocalDateTime todayStart = now.toLocalDate().atStartOfDay();
                LocalDateTime todayEnd = todayStart.plusDays(1);
                schedules = scheduleRepository.findByUserIdAndDateRange(userId, todayStart, todayEnd);
                break;
            case "week":
                LocalDateTime weekStart = now.toLocalDate().atStartOfDay();
                LocalDateTime weekEnd = weekStart.plusWeeks(1);
                schedules = scheduleRepository.findByUserIdAndDateRange(userId, weekStart, weekEnd);
                break;
            default:
                schedules = scheduleRepository.findUpcomingSchedules(userId, now);
        }

        if (schedules.isEmpty()) {
            return "예정된 일정이 없습니다.";
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM월 dd일 HH:mm");
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%d개의 일정:\n", schedules.size()));

        for (Schedule s : schedules) {
            sb.append(String.format("- %s (%s)",
                s.getTitle(),
                s.getStartTime().format(formatter)
            ));
            if (s.getLocation() != null) {
                sb.append(" @ ").append(s.getLocation());
            }
            sb.append("\n");
        }

        return sb.toString();
    }

    private String addNoteTool(Map<String, Object> params) {
        Long userId = (Long) params.get("userId");

        Note note = new Note();
        note.setUserId(userId);
        note.setTitle((String) params.getOrDefault("title", "새 메모"));
        note.setContent((String) params.get("content"));
        note.setCategory((String) params.get("category"));
        note.setIsPinned(false);

        Note saved = noteRepository.save(note);

        return String.format("메모 '%s'이(가) 저장되었습니다.", saved.getTitle());
    }

    private String searchNotesTool(Map<String, Object> params) {
        Long userId = (Long) params.get("userId");
        String keyword = (String) params.get("keyword");
        String category = (String) params.get("category");

        List<Note> notes;
        if (keyword != null) {
            notes = noteRepository.searchNotes(userId, keyword);
        } else if (category != null) {
            notes = noteRepository.findByUserIdAndCategoryOrderByCreatedAtDesc(userId, category);
        } else {
            notes = noteRepository.findByUserIdOrderByCreatedAtDesc(userId);
        }

        if (notes.isEmpty()) {
            return "검색 결과가 없습니다.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%d개의 메모:\n", notes.size()));

        for (Note n : notes) {
            sb.append(String.format("- %s", n.getTitle()));
            if (n.getCategory() != null) {
                sb.append(" [").append(n.getCategory()).append("]");
            }
            sb.append("\n");
        }

        return sb.toString();
    }

    private String addTodoTool(Map<String, Object> params) {
        Long userId = (Long) params.get("userId");

        Todo todo = new Todo();
        todo.setUserId(userId);
        todo.setTitle((String) params.get("title"));
        todo.setDescription((String) params.get("description"));

        // Priority 설정
        String priorityStr = (String) params.getOrDefault("priority", "medium");
        try {
            todo.setPriority(Todo.Priority.valueOf(priorityStr.toUpperCase()));
        } catch (IllegalArgumentException e) {
            // 기본값으로 MEDIUM 설정
            log.warn("Invalid priority value: {}. Using MEDIUM as default.", priorityStr);
            todo.setPriority(Todo.Priority.MEDIUM);
        }

        // Due date 파싱
        String dueDateStr = (String) params.get("due_date");
        if (dueDateStr != null) {
            todo.setDueDate(LocalDate.parse(dueDateStr));
        }

        todo.setStatus(Todo.TodoStatus.PENDING);

        Todo saved = todoRepository.save(todo);

        return String.format("할 일 '%s'이(가) 추가되었습니다. (우선순위: %s)",
            saved.getTitle(), saved.getPriority());
    }

    private String getTodosTool(Map<String, Object> params) {
        Long userId = (Long) params.get("userId");
        String status = (String) params.get("status");

        List<Todo> todos;
        if ("pending".equalsIgnoreCase(status)) {
            todos = todoRepository.findByUserIdAndStatusOrderByPriorityDescDueDateAsc(
                userId, Todo.TodoStatus.PENDING);
        } else if ("completed".equalsIgnoreCase(status)) {
            todos = todoRepository.findByUserIdAndStatusOrderByPriorityDescDueDateAsc(
                userId, Todo.TodoStatus.COMPLETED);
        } else {
            todos = todoRepository.findByUserIdOrderByDueDateAsc(userId);
        }

        if (todos.isEmpty()) {
            return "할 일이 없습니다.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%d개의 할 일:\n", todos.size()));

        for (Todo t : todos) {
            sb.append(t.getStatus() == Todo.TodoStatus.COMPLETED ? "✓ " : "☐ ");
            sb.append(t.getTitle());
            if (t.getPriority() == Todo.Priority.HIGH) {
                sb.append(" ❗");
            }
            if (t.getDueDate() != null) {
                sb.append(" (").append(t.getDueDate()).append(")");
            }
            sb.append("\n");
        }

        return sb.toString();
    }

    private String completeTodoTool(Map<String, Object> params) {
        Long userId = (Long) params.get("userId");
        String todoIdStr = (String) params.get("todo_id");
        String title = (String) params.get("title");

        Todo todo = null;

        // ID로 찾기
        if (todoIdStr != null) {
            try {
                Long todoId = Long.parseLong(todoIdStr);
                todo = todoRepository.findById(todoId).orElse(null);
            } catch (NumberFormatException e) {
                // ID가 숫자가 아니면 제목으로 검색
                title = todoIdStr;
            }
        }

        // 제목으로 찾기
        if (todo == null && title != null) {
            List<Todo> todos = todoRepository.findByUserIdAndStatusOrderByPriorityDescDueDateAsc(
                userId, Todo.TodoStatus.PENDING);
            for (Todo t : todos) {
                if (t.getTitle().contains(title)) {
                    todo = t;
                    break;
                }
            }
        }

        if (todo == null) {
            return "해당 할 일을 찾을 수 없습니다.";
        }

        todo.setStatus(Todo.TodoStatus.COMPLETED);
        todo.setCompletedAt(LocalDateTime.now());
        Todo saved = todoRepository.save(todo);

        return String.format("'%s' 할 일이 완료되었습니다!", saved.getTitle());
    }

    /**
     * 날짜/시간 파싱 유틸리티
     */
    private LocalDateTime parseDateTime(String dateStr, String timeStr) {
        LocalDateTime result = LocalDateTime.now();

        // 날짜 파싱
        if (dateStr != null) {
            String lowerDate = dateStr.toLowerCase();
            if (lowerDate.contains("오늘") || lowerDate.contains("today")) {
                result = LocalDate.now().atStartOfDay();
            } else if (lowerDate.contains("내일") || lowerDate.contains("tomorrow")) {
                result = LocalDate.now().plusDays(1).atStartOfDay();
            } else if (lowerDate.contains("모레")) {
                result = LocalDate.now().plusDays(2).atStartOfDay();
            } else {
                try {
                    result = LocalDate.parse(dateStr).atStartOfDay();
                } catch (Exception e) {
                    // 파싱 실패 시 현재 날짜 유지
                }
            }
        }

        // 시간 파싱
        if (timeStr != null) {
            try {
                String[] parts = timeStr.split(":");
                if (parts.length >= 1) {
                    int hour = Integer.parseInt(parts[0].replaceAll("[^0-9]", ""));
                    int minute = parts.length >= 2 ? 
                        Integer.parseInt(parts[1].replaceAll("[^0-9]", "")) : 0;
                    result = result.withHour(hour).withMinute(minute);
                }
            } catch (Exception e) {
                // 파싱 실패 시 오전 9시 기본값
                result = result.withHour(9).withMinute(0);
            }
        } else {
            result = result.withHour(9).withMinute(0);
        }

        return result;
    }

    /**
     * 도구 파라미터 클래스
     */
    private static class ToolParameter {
        String name;
        String description;
        boolean required;

        ToolParameter(String name, String description, boolean required) {
            this.name = name;
            this.description = description;
            this.required = required;
        }
    }

    /**
     * 도구 실행 결과 클래스
     */
    private static class ToolExecutionResult {
        ToolExecutionRequest request;
        String output;

        ToolExecutionResult(ToolExecutionRequest request, String output) {
            this.request = request;
            this.output = output;
        }
    }

}
