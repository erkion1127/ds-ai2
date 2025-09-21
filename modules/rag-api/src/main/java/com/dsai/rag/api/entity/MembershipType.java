package com.dsai.rag.api.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "membership_types")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "membershipPurchases")
public class MembershipType {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "type_code", unique = true, nullable = false, length = 50)
    private String typeCode;
    
    @Column(name = "type_name", nullable = false, length = 100)
    private String typeName;
    
    @Column(length = 20)
    @Enumerated(EnumType.STRING)
    private MembershipCategory category;
    
    @Column(name = "duration_months")
    private Integer durationMonths;
    
    @Column(name = "session_count")
    private Integer sessionCount;
    
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;
    
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @OneToMany(mappedBy = "membershipType", fetch = FetchType.LAZY)
    @Builder.Default
    private List<MembershipPurchase> membershipPurchases = new ArrayList<>();
    
    public enum MembershipCategory {
        GYM("헬스"),
        PT("PT"),
        PILATES("필라테스"),
        YOGA("요가"),
        SWIMMING("수영"),
        PACKAGE("패키지");
        
        private final String description;
        
        MembershipCategory(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
}