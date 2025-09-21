package com.dsai.rag.api.repository;

import com.dsai.rag.api.entity.Todo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface TodoRepository extends JpaRepository<Todo, Long> {
    
    List<Todo> findByUserIdOrderByDueDateAsc(Long userId);
    
    List<Todo> findByUserIdAndStatusOrderByPriorityDescDueDateAsc(Long userId, Todo.TodoStatus status);
    
    @Query("SELECT t FROM Todo t WHERE t.userId = :userId AND t.dueDate <= :date AND t.status != 'COMPLETED'")
    List<Todo> findOverdueTodos(@Param("userId") Long userId, @Param("date") LocalDate date);
    
    @Query("SELECT t FROM Todo t WHERE t.userId = :userId AND t.dueDate = :date")
    List<Todo> findTodosByDueDate(@Param("userId") Long userId, @Param("date") LocalDate date);
    
    @Query("SELECT t FROM Todo t WHERE t.userId = :userId AND t.priority = :priority ORDER BY t.dueDate")
    List<Todo> findByPriority(@Param("userId") Long userId, @Param("priority") Todo.Priority priority);
}