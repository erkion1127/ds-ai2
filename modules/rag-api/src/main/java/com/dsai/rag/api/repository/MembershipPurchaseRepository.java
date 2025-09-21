package com.dsai.rag.api.repository;

import com.dsai.rag.api.entity.MembershipPurchase;
import com.dsai.rag.api.entity.MembershipPurchase.PurchaseStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public interface MembershipPurchaseRepository extends JpaRepository<MembershipPurchase, Long> {
    
    Optional<MembershipPurchase> findByPurchaseNo(String purchaseNo);
    
    List<MembershipPurchase> findByMemberId(Long memberId);
    
    Page<MembershipPurchase> findByMemberId(Long memberId, Pageable pageable);
    
    List<MembershipPurchase> findByMemberIdAndStatus(Long memberId, PurchaseStatus status);
    
    Page<MembershipPurchase> findByMemberIdAndStatus(Long memberId, PurchaseStatus status, Pageable pageable);
    
    List<MembershipPurchase> findByStatus(PurchaseStatus status);
    
    Long countByStatus(PurchaseStatus status);
    
    List<MembershipPurchase> findByPurchaseDateBetween(LocalDate startDate, LocalDate endDate);
    
    // 활성 회원권 조회
    @Query("SELECT mp FROM MembershipPurchase mp WHERE mp.member.id = :memberId " +
           "AND mp.status = 'ACTIVE' AND mp.endDate >= :currentDate")
    List<MembershipPurchase> findActiveMemberships(@Param("memberId") Long memberId, 
                                                   @Param("currentDate") LocalDate currentDate);
    
    // 만료 예정 회원권 조회 (30일 이내)
    @Query("SELECT mp FROM MembershipPurchase mp WHERE mp.status = 'ACTIVE' " +
           "AND mp.endDate BETWEEN :currentDate AND :expiryDate " +
           "ORDER BY mp.endDate")
    List<MembershipPurchase> findExpiringMemberships(@Param("currentDate") LocalDate currentDate,
                                                     @Param("expiryDate") LocalDate expiryDate);
    
    // PT/필라테스 잔여 횟수가 있는 회원권 조회
    @Query("SELECT mp FROM MembershipPurchase mp " +
           "JOIN mp.membershipType mt " +
           "WHERE mp.member.id = :memberId " +
           "AND mt.category IN ('PT', 'PILATES') " +
           "AND mp.status = 'ACTIVE' " +
           "AND mp.remainingSessions > 0")
    List<MembershipPurchase> findMembershipsWithRemainingSessions(@Param("memberId") Long memberId);
    
    // 월별 매출 통계
    @Query(value = "SELECT " +
           "YEAR(purchase_date) as year, " +
           "MONTH(purchase_date) as month, " +
           "COUNT(*) as purchaseCount, " +
           "SUM(final_price) as totalRevenue " +
           "FROM membership_purchases " +
           "WHERE purchase_date BETWEEN :startDate AND :endDate " +
           "GROUP BY YEAR(purchase_date), MONTH(purchase_date) " +
           "ORDER BY year DESC, month DESC", 
           nativeQuery = true)
    List<Map<String, Object>> getMonthlySalesStatistics(@Param("startDate") LocalDate startDate,
                                                        @Param("endDate") LocalDate endDate);
    
    // 회원권 종류별 판매 통계
    @Query(value = "SELECT " +
           "mt.type_name as typeName, " +
           "mt.category as category, " +
           "COUNT(mp.id) as salesCount, " +
           "SUM(mp.final_price) as totalRevenue " +
           "FROM membership_purchases mp " +
           "JOIN membership_types mt ON mp.membership_type_id = mt.id " +
           "WHERE mp.purchase_date BETWEEN :startDate AND :endDate " +
           "GROUP BY mt.id, mt.type_name, mt.category " +
           "ORDER BY totalRevenue DESC",
           nativeQuery = true)
    List<Map<String, Object>> getMembershipTypeSalesStatistics(@Param("startDate") LocalDate startDate,
                                                               @Param("endDate") LocalDate endDate);
}