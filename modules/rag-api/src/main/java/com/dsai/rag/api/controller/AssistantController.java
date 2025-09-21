package com.dsai.rag.api.controller;

import com.dsai.rag.api.entity.*;
import com.dsai.rag.api.repository.*;
import com.dsai.rag.api.service.PersonalAssistantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/assistant")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Slf4j
public class AssistantController {
    
    private final PersonalAssistantService assistantService;
    private final ScheduleRepository scheduleRepository;
    private final NoteRepository noteRepository;
    private final TodoRepository todoRepository;
    
    // 대화형 인터페이스
    @PostMapping("/chat")
    public ResponseEntity<?> chat(@RequestBody Map<String, Object> request) {
        Long userId = request.containsKey("userId") ? 
            ((Number) request.get("userId")).longValue() : 1L;
        String message = (String) request.get("message");
        String sessionId = (String) request.get("sessionId");
        
        Map<String, Object> response;
        if (sessionId != null && !sessionId.isEmpty()) {
            response = assistantService.processMessage(userId, message, sessionId);
        } else {
            response = assistantService.processMessage(userId, message);
        }
        
        return ResponseEntity.ok(response);
    }
    
    // 일정 관리 API
    @GetMapping("/schedules")
    public ResponseEntity<List<Schedule>> getSchedules(@RequestParam(defaultValue = "1") Long userId) {
        List<Schedule> schedules = scheduleRepository.findByUserIdOrderByStartTimeDesc(userId);
        return ResponseEntity.ok(schedules);
    }
    
    @GetMapping("/schedules/today")
    public ResponseEntity<List<Schedule>> getTodaySchedules(@RequestParam(defaultValue = "1") Long userId) {
        LocalDateTime start = LocalDate.now().atStartOfDay();
        LocalDateTime end = start.plusDays(1);
        List<Schedule> schedules = scheduleRepository.findByUserIdAndDateRange(userId, start, end);
        return ResponseEntity.ok(schedules);
    }
    
    @GetMapping("/schedules/upcoming")
    public ResponseEntity<List<Schedule>> getUpcomingSchedules(@RequestParam(defaultValue = "1") Long userId) {
        List<Schedule> schedules = scheduleRepository.findUpcomingSchedules(userId, LocalDateTime.now());
        return ResponseEntity.ok(schedules);
    }
    
    @PostMapping("/schedules")
    public ResponseEntity<Schedule> createSchedule(@RequestBody Schedule schedule) {
        schedule.setUserId(1L); // 임시
        Schedule saved = scheduleRepository.save(schedule);
        return ResponseEntity.ok(saved);
    }
    
    @PutMapping("/schedules/{id}")
    public ResponseEntity<Schedule> updateSchedule(@PathVariable Long id, @RequestBody Schedule schedule) {
        Schedule existing = scheduleRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Schedule not found"));
        
        existing.setTitle(schedule.getTitle());
        existing.setDescription(schedule.getDescription());
        existing.setStartTime(schedule.getStartTime());
        existing.setEndTime(schedule.getEndTime());
        existing.setLocation(schedule.getLocation());
        
        Schedule updated = scheduleRepository.save(existing);
        return ResponseEntity.ok(updated);
    }
    
    @DeleteMapping("/schedules/{id}")
    public ResponseEntity<?> deleteSchedule(@PathVariable Long id) {
        scheduleRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }
    
    // 메모 관리 API
    @GetMapping("/notes")
    public ResponseEntity<List<Note>> getNotes(@RequestParam(defaultValue = "1") Long userId) {
        List<Note> notes = noteRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return ResponseEntity.ok(notes);
    }
    
    @GetMapping("/notes/search")
    public ResponseEntity<List<Note>> searchNotes(
            @RequestParam(defaultValue = "1") Long userId,
            @RequestParam String keyword) {
        List<Note> notes = noteRepository.searchNotes(userId, keyword);
        return ResponseEntity.ok(notes);
    }
    
    @GetMapping("/notes/pinned")
    public ResponseEntity<List<Note>> getPinnedNotes(@RequestParam(defaultValue = "1") Long userId) {
        List<Note> notes = noteRepository.findByUserIdAndIsPinnedTrueOrderByCreatedAtDesc(userId);
        return ResponseEntity.ok(notes);
    }
    
    @PostMapping("/notes")
    public ResponseEntity<Note> createNote(@RequestBody Note note) {
        note.setUserId(1L); // 임시
        Note saved = noteRepository.save(note);
        return ResponseEntity.ok(saved);
    }
    
    @PutMapping("/notes/{id}")
    public ResponseEntity<Note> updateNote(@PathVariable Long id, @RequestBody Note note) {
        Note existing = noteRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Note not found"));
        
        existing.setTitle(note.getTitle());
        existing.setContent(note.getContent());
        existing.setCategory(note.getCategory());
        existing.setTags(note.getTags());
        existing.setIsPinned(note.getIsPinned());
        
        Note updated = noteRepository.save(existing);
        return ResponseEntity.ok(updated);
    }
    
    @DeleteMapping("/notes/{id}")
    public ResponseEntity<?> deleteNote(@PathVariable Long id) {
        noteRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }
    
    // TODO 관리 API
    @GetMapping("/todos")
    public ResponseEntity<List<Todo>> getTodos(@RequestParam(defaultValue = "1") Long userId) {
        List<Todo> todos = todoRepository.findByUserIdOrderByDueDateAsc(userId);
        return ResponseEntity.ok(todos);
    }
    
    @GetMapping("/todos/pending")
    public ResponseEntity<List<Todo>> getPendingTodos(@RequestParam(defaultValue = "1") Long userId) {
        List<Todo> todos = todoRepository.findByUserIdAndStatusOrderByPriorityDescDueDateAsc(
            userId, Todo.TodoStatus.PENDING
        );
        return ResponseEntity.ok(todos);
    }
    
    @GetMapping("/todos/overdue")
    public ResponseEntity<List<Todo>> getOverdueTodos(@RequestParam(defaultValue = "1") Long userId) {
        List<Todo> todos = todoRepository.findOverdueTodos(userId, LocalDate.now());
        return ResponseEntity.ok(todos);
    }
    
    @PostMapping("/todos")
    public ResponseEntity<Todo> createTodo(@RequestBody Todo todo) {
        todo.setUserId(1L); // 임시
        Todo saved = todoRepository.save(todo);
        return ResponseEntity.ok(saved);
    }
    
    @PutMapping("/todos/{id}")
    public ResponseEntity<Todo> updateTodo(@PathVariable Long id, @RequestBody Todo todo) {
        Todo existing = todoRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Todo not found"));
        
        existing.setTitle(todo.getTitle());
        existing.setDescription(todo.getDescription());
        existing.setDueDate(todo.getDueDate());
        existing.setPriority(todo.getPriority());
        existing.setStatus(todo.getStatus());
        
        Todo updated = todoRepository.save(existing);
        return ResponseEntity.ok(updated);
    }
    
    @PutMapping("/todos/{id}/complete")
    public ResponseEntity<Todo> completeTodo(@PathVariable Long id) {
        Todo todo = todoRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Todo not found"));
        
        todo.setStatus(Todo.TodoStatus.COMPLETED);
        todo.setCompletedAt(LocalDateTime.now());
        
        Todo updated = todoRepository.save(todo);
        return ResponseEntity.ok(updated);
    }
    
    @DeleteMapping("/todos/{id}")
    public ResponseEntity<?> deleteTodo(@PathVariable Long id) {
        todoRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }
    
    // 대시보드 통계
    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getDashboard(@RequestParam(defaultValue = "1") Long userId) {
        Map<String, Object> dashboard = new HashMap<>();
        
        // 오늘 일정
        LocalDateTime start = LocalDate.now().atStartOfDay();
        LocalDateTime end = start.plusDays(1);
        List<Schedule> todaySchedules = scheduleRepository.findByUserIdAndDateRange(userId, start, end);
        dashboard.put("todaySchedules", todaySchedules);
        
        // 미완료 TODO
        List<Todo> pendingTodos = todoRepository.findByUserIdAndStatusOrderByPriorityDescDueDateAsc(
            userId, Todo.TodoStatus.PENDING
        );
        dashboard.put("pendingTodos", pendingTodos);
        
        // 최근 메모
        List<Note> recentNotes = noteRepository.findByUserIdOrderByCreatedAtDesc(userId);
        if (recentNotes.size() > 5) {
            recentNotes = recentNotes.subList(0, 5);
        }
        dashboard.put("recentNotes", recentNotes);
        
        // 통계
        Map<String, Integer> stats = new HashMap<>();
        stats.put("totalSchedules", scheduleRepository.findByUserIdOrderByStartTimeDesc(userId).size());
        stats.put("totalNotes", noteRepository.findByUserIdOrderByCreatedAtDesc(userId).size());
        stats.put("totalTodos", todoRepository.findByUserIdOrderByDueDateAsc(userId).size());
        stats.put("completedTodos", todoRepository.findByUserIdAndStatusOrderByPriorityDescDueDateAsc(
            userId, Todo.TodoStatus.COMPLETED).size());
        dashboard.put("stats", stats);
        
        return ResponseEntity.ok(dashboard);
    }
}