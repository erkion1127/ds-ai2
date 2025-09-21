package com.dsai.rag.api.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "trainers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "sessionUsages")
public class Trainer {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "trainer_code", unique = true, nullable = false, length = 50)
    private String trainerCode;
    
    @Column(nullable = false, length = 100)
    private String name;
    
    @Column(length = 20)
    private String phone;
    
    @Column(length = 100)
    private String email;
    
    @Column(length = 200)
    private String specialty;
    
    @Column(name = "hire_date")
    private LocalDate hireDate;
    
    @Column(length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private TrainerStatus status = TrainerStatus.ACTIVE;
    
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @OneToMany(mappedBy = "trainer", fetch = FetchType.LAZY)
    @Builder.Default
    private List<SessionUsage> sessionUsages = new ArrayList<>();
    
    public enum TrainerStatus {
        ACTIVE("활동중"),
        INACTIVE("비활성"),
        ON_LEAVE("휴직중");
        
        private final String description;
        
        TrainerStatus(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
}