package com.dsai.rag.api.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "membership_purchases")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"member", "membershipType", "sessionUsages"})
public class MembershipPurchase {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "purchase_no", unique = true, nullable = false, length = 50)
    private String purchaseNo;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "membership_type_id", nullable = false)
    private MembershipType membershipType;
    
    @Column(name = "purchase_date", nullable = false)
    private LocalDate purchaseDate;
    
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;
    
    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;
    
    @Column(name = "original_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal originalPrice;
    
    @Column(name = "discount_amount", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO;
    
    @Column(name = "final_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal finalPrice;
    
    @Column(name = "payment_method", length = 50)
    private String paymentMethod;
    
    @Column(name = "remaining_sessions")
    private Integer remainingSessions;
    
    @Column(length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private PurchaseStatus status = PurchaseStatus.ACTIVE;
    
    @Column(name = "suspension_start_date")
    private LocalDate suspensionStartDate;
    
    @Column(name = "suspension_end_date")
    private LocalDate suspensionEndDate;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transfer_to_member_id")
    private Member transferToMember;
    
    @Column(name = "transfer_date")
    private LocalDate transferDate;
    
    @Column(name = "cancellation_date")
    private LocalDate cancellationDate;
    
    @Column(name = "cancellation_reason", columnDefinition = "TEXT")
    private String cancellationReason;
    
    @Column(columnDefinition = "TEXT")
    private String notes;
    
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @OneToMany(mappedBy = "membershipPurchase", fetch = FetchType.LAZY)
    @Builder.Default
    private List<SessionUsage> sessionUsages = new ArrayList<>();
    
    public enum PurchaseStatus {
        ACTIVE("활성"),
        EXPIRED("만료"),
        SUSPENDED("일시정지"),
        CANCELLED("취소"),
        TRANSFERRED("양도");
        
        private final String description;
        
        PurchaseStatus(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    // 비즈니스 로직 메서드
    public boolean isActive() {
        return status == PurchaseStatus.ACTIVE && 
               LocalDate.now().isBefore(endDate.plusDays(1));
    }
    
    public boolean isExpired() {
        return LocalDate.now().isAfter(endDate);
    }
    
    public int getDaysRemaining() {
        if (isExpired()) return 0;
        return (int) java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), endDate);
    }
    
    public void useSession() {
        if (remainingSessions != null && remainingSessions > 0) {
            remainingSessions--;
        }
    }
}