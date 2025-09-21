package com.dsai.rag.api.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "session_usage")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"membershipPurchase", "member", "trainer"})
public class SessionUsage {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "membership_purchase_id", nullable = false)
    private MembershipPurchase membershipPurchase;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trainer_id")
    private Trainer trainer;
    
    @Column(name = "usage_date", nullable = false)
    private LocalDateTime usageDate;
    
    @Column(name = "session_type", length = 20)
    @Enumerated(EnumType.STRING)
    private SessionType sessionType;
    
    @Column(name = "duration_minutes")
    @Builder.Default
    private Integer durationMinutes = 60;
    
    @Column(columnDefinition = "TEXT")
    private String notes;
    
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    public enum SessionType {
        PT("퍼스널 트레이닝"),
        PILATES("필라테스"),
        YOGA("요가"),
        OTHER("기타");
        
        private final String description;
        
        SessionType(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
}