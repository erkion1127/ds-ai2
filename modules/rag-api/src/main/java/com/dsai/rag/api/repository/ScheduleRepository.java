package com.dsai.rag.api.repository;

import com.dsai.rag.api.entity.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ScheduleRepository extends JpaRepository<Schedule, Long> {
    
    List<Schedule> findByUserIdOrderByStartTimeDesc(Long userId);
    
    @Query("SELECT s FROM Schedule s WHERE s.userId = :userId AND s.startTime BETWEEN :start AND :end ORDER BY s.startTime")
    List<Schedule> findByUserIdAndDateRange(@Param("userId") Long userId, 
                                           @Param("start") LocalDateTime start, 
                                           @Param("end") LocalDateTime end);
    
    @Query("SELECT s FROM Schedule s WHERE s.userId = :userId AND s.startTime >= :now AND s.status = 'scheduled' ORDER BY s.startTime")
    List<Schedule> findUpcomingSchedules(@Param("userId") Long userId, @Param("now") LocalDateTime now);
    
    @Query("SELECT s FROM Schedule s WHERE s.reminderTime IS NOT NULL AND s.reminderTime <= :now AND s.status = 'scheduled'")
    List<Schedule> findSchedulesNeedingReminders(@Param("now") LocalDateTime now);
}