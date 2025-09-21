package com.dsai.rag.api.repository;

import com.dsai.rag.api.entity.Note;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NoteRepository extends JpaRepository<Note, Long> {
    
    List<Note> findByUserIdOrderByCreatedAtDesc(Long userId);
    
    List<Note> findByUserIdAndCategoryOrderByCreatedAtDesc(Long userId, String category);
    
    List<Note> findByUserIdAndIsPinnedTrueOrderByCreatedAtDesc(Long userId);
    
    @Query("SELECT n FROM Note n WHERE n.userId = :userId AND (LOWER(n.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(n.content) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<Note> searchNotes(@Param("userId") Long userId, @Param("keyword") String keyword);
    
    @Query("SELECT n FROM Note n WHERE n.userId = :userId AND n.tags LIKE CONCAT('%', :tag, '%')")
    List<Note> findByTag(@Param("userId") Long userId, @Param("tag") String tag);
}