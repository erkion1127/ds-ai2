package com.dsai.rag.api.repository;

import com.dsai.rag.api.entity.SalesTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Repository
public interface SalesTransactionRepository extends JpaRepository<SalesTransaction, Long> {
    
    List<SalesTransaction> findByTransactionDateBetween(LocalDate startDate, LocalDate endDate);
    
    List<SalesTransaction> findByMemberId(Long memberId);
    
    List<SalesTransaction> findByProductId(Long productId);
    
    @Query("SELECT SUM(st.finalAmount) FROM SalesTransaction st WHERE st.transactionDate = :date")
    BigDecimal getTotalSalesByDate(@Param("date") LocalDate date);
    
    @Query("SELECT SUM(st.finalAmount) FROM SalesTransaction st WHERE st.transactionDate BETWEEN :startDate AND :endDate")
    BigDecimal getTotalSalesBetweenDates(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
    
    @Query(value = "SELECT DATE(transaction_date) as date, " +
            "COUNT(*) as count, " +
            "SUM(final_amount) as total, " +
            "AVG(final_amount) as average " +
            "FROM sales_transactions " +
            "WHERE transaction_date BETWEEN :startDate AND :endDate " +
            "GROUP BY DATE(transaction_date) " +
            "ORDER BY date DESC", nativeQuery = true)
    List<Map<String, Object>> getDailySalesStatistics(@Param("startDate") LocalDate startDate, 
                                                      @Param("endDate") LocalDate endDate);
    
    @Query(value = "SELECT p.product_name as productName, " +
            "p.product_type as productType, " +
            "COUNT(st.id) as salesCount, " +
            "SUM(st.final_amount) as totalAmount " +
            "FROM sales_transactions st " +
            "JOIN products p ON st.product_id = p.id " +
            "WHERE st.transaction_date BETWEEN :startDate AND :endDate " +
            "GROUP BY p.id, p.product_name, p.product_type " +
            "ORDER BY totalAmount DESC", nativeQuery = true)
    List<Map<String, Object>> getProductSalesStatistics(@Param("startDate") LocalDate startDate, 
                                                        @Param("endDate") LocalDate endDate);
    
    @Query(value = "SELECT m.member_name as memberName, " +
            "COUNT(st.id) as purchaseCount, " +
            "SUM(st.final_amount) as totalSpent " +
            "FROM sales_transactions st " +
            "JOIN members m ON st.member_id = m.id " +
            "WHERE st.transaction_date BETWEEN :startDate AND :endDate " +
            "GROUP BY m.id, m.member_name " +
            "ORDER BY totalSpent DESC " +
            "LIMIT 10", nativeQuery = true)
    List<Map<String, Object>> getTopMembers(@Param("startDate") LocalDate startDate, 
                                           @Param("endDate") LocalDate endDate);
}