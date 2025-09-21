package com.dsai.rag.api.service;

import com.dsai.rag.api.repository.MemberRepository;
import com.dsai.rag.api.repository.ProductRepository;
import com.dsai.rag.api.repository.SalesTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DoublessDashboardService {
    
    private final SalesTransactionRepository salesTransactionRepository;
    private final MemberRepository memberRepository;
    private final ProductRepository productRepository;
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    
    public Map<String, Object> getDashboardData(LocalDate startDate, LocalDate endDate) {
        Map<String, Object> dashboard = new HashMap<>();
        
        try {
            // 기간 정보
            dashboard.put("period", Map.of(
                "startDate", startDate.toString(),
                "endDate", endDate.toString()
            ));
            
            // 총 매출 정보
            BigDecimal totalSales = salesTransactionRepository.getTotalSalesBetweenDates(startDate, endDate);
            if (totalSales == null) totalSales = BigDecimal.ZERO;
            
            // 이전 기간 대비 증감률 계산
            long daysBetween = endDate.toEpochDay() - startDate.toEpochDay() + 1;
            LocalDate prevStartDate = startDate.minusDays(daysBetween);
            LocalDate prevEndDate = startDate.minusDays(1);
            BigDecimal prevTotalSales = salesTransactionRepository.getTotalSalesBetweenDates(prevStartDate, prevEndDate);
            if (prevTotalSales == null) prevTotalSales = BigDecimal.ZERO;
            
            BigDecimal growthRate = BigDecimal.ZERO;
            if (prevTotalSales.compareTo(BigDecimal.ZERO) > 0) {
                growthRate = totalSales.subtract(prevTotalSales)
                    .divide(prevTotalSales, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));
            }
            
            dashboard.put("totalSales", Map.of(
                "amount", totalSales,
                "formatted", formatCurrency(totalSales),
                "growthRate", growthRate,
                "growthRateFormatted", growthRate.setScale(2, RoundingMode.HALF_UP) + "%"
            ));
            
            // 거래 건수 정보
            long transactionCount = salesTransactionRepository.findByTransactionDateBetween(startDate, endDate).size();
            dashboard.put("transactionCount", transactionCount);
            
            // 평균 거래 금액
            BigDecimal avgTransaction = transactionCount > 0 
                ? totalSales.divide(new BigDecimal(transactionCount), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
            dashboard.put("averageTransaction", Map.of(
                "amount", avgTransaction,
                "formatted", formatCurrency(avgTransaction)
            ));
            
            // 활성 회원 수
            Long activeMemberCount = memberRepository.countActiveMembers();
            dashboard.put("activeMemberCount", activeMemberCount);
            
            // 일별 매출 트렌드 (최대 30일)
            List<Map<String, Object>> dailySales = getDailySalesData(startDate, endDate);
            dashboard.put("dailySalesTrend", dailySales);
            
            // 상품별 매출 분포
            List<Map<String, Object>> productSales = getProductSalesData(startDate, endDate);
            dashboard.put("productSalesDistribution", productSales);
            
            // 상위 회원
            List<Map<String, Object>> topMembers = getTopMembersData(startDate, endDate);
            dashboard.put("topMembers", topMembers);
            
            // 결제 방법별 분포
            Map<String, Object> paymentMethods = getPaymentMethodDistribution(startDate, endDate);
            dashboard.put("paymentMethodDistribution", paymentMethods);
            
        } catch (Exception e) {
            log.error("Error building dashboard data", e);
            throw new RuntimeException("Failed to build dashboard data: " + e.getMessage(), e);
        }
        
        return dashboard;
    }
    
    public List<Map<String, Object>> getDailySales(LocalDate startDate, LocalDate endDate) {
        List<Map<String, Object>> dailyStats = salesTransactionRepository.getDailySalesStatistics(startDate, endDate);
        
        // 결과 포맷팅
        return dailyStats.stream()
            .map(stat -> {
                Map<String, Object> formatted = new HashMap<>();
                formatted.put("date", stat.get("date").toString());
                formatted.put("count", stat.get("count"));
                formatted.put("total", stat.get("total"));
                formatted.put("average", stat.get("average"));
                formatted.put("totalFormatted", formatCurrency((BigDecimal) stat.get("total")));
                formatted.put("averageFormatted", formatCurrency((BigDecimal) stat.get("average")));
                return formatted;
            })
            .collect(Collectors.toList());
    }
    
    public List<Map<String, Object>> getProductSales(LocalDate startDate, LocalDate endDate) {
        List<Map<String, Object>> productStats = salesTransactionRepository.getProductSalesStatistics(startDate, endDate);
        
        // 결과 포맷팅 및 백분율 계산
        BigDecimal totalAmount = productStats.stream()
            .map(stat -> (BigDecimal) stat.get("totalAmount"))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        return productStats.stream()
            .map(stat -> {
                Map<String, Object> formatted = new HashMap<>();
                formatted.put("productName", stat.get("productName"));
                formatted.put("productType", stat.get("productType"));
                formatted.put("salesCount", stat.get("salesCount"));
                BigDecimal amount = (BigDecimal) stat.get("totalAmount");
                formatted.put("totalAmount", amount);
                formatted.put("totalAmountFormatted", formatCurrency(amount));
                
                if (totalAmount.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal percentage = amount.divide(totalAmount, 4, RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("100"));
                    formatted.put("percentage", percentage.setScale(2, RoundingMode.HALF_UP));
                } else {
                    formatted.put("percentage", BigDecimal.ZERO);
                }
                
                return formatted;
            })
            .collect(Collectors.toList());
    }
    
    public List<Map<String, Object>> getTopMembers(LocalDate startDate, LocalDate endDate) {
        List<Map<String, Object>> topMembers = salesTransactionRepository.getTopMembers(startDate, endDate);
        
        // 결과 포맷팅 및 순위 추가
        int rank = 1;
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> member : topMembers) {
            Map<String, Object> formatted = new HashMap<>();
            formatted.put("rank", rank++);
            formatted.put("memberName", member.get("memberName"));
            formatted.put("purchaseCount", member.get("purchaseCount"));
            BigDecimal totalSpent = (BigDecimal) member.get("totalSpent");
            formatted.put("totalSpent", totalSpent);
            formatted.put("totalSpentFormatted", formatCurrency(totalSpent));
            
            // 평균 구매액 계산
            Long count = (Long) member.get("purchaseCount");
            if (count > 0) {
                BigDecimal avgPurchase = totalSpent.divide(new BigDecimal(count), 2, RoundingMode.HALF_UP);
                formatted.put("averagePurchase", avgPurchase);
                formatted.put("averagePurchaseFormatted", formatCurrency(avgPurchase));
            } else {
                formatted.put("averagePurchase", BigDecimal.ZERO);
                formatted.put("averagePurchaseFormatted", formatCurrency(BigDecimal.ZERO));
            }
            
            result.add(formatted);
        }
        
        return result;
    }
    
    public List<Map<String, Object>> getRecentTransactions(int limit) {
        // 최근 거래 조회 (페이지네이션 사용)
        var transactions = salesTransactionRepository.findAll();
        
        // 날짜 기준 정렬 후 제한된 수만큼 반환
        return transactions.stream()
            .sorted((t1, t2) -> t2.getTransactionDate().compareTo(t1.getTransactionDate()))
            .limit(limit)
            .map(t -> {
                Map<String, Object> formatted = new HashMap<>();
                formatted.put("transactionNo", t.getTransactionNo());
                formatted.put("date", t.getTransactionDate().toString());
                formatted.put("memberName", t.getMember() != null ? t.getMember().getMemberName() : "Unknown");
                formatted.put("productName", t.getProduct() != null ? t.getProduct().getProductName() : "Unknown");
                formatted.put("amount", t.getFinalAmount());
                formatted.put("amountFormatted", formatCurrency(t.getFinalAmount()));
                formatted.put("paymentMethod", t.getPaymentMethod());
                formatted.put("paymentStatus", t.getPaymentStatus());
                return formatted;
            })
            .collect(Collectors.toList());
    }
    
    private List<Map<String, Object>> getDailySalesData(LocalDate startDate, LocalDate endDate) {
        List<Map<String, Object>> dailyStats = salesTransactionRepository.getDailySalesStatistics(startDate, endDate);
        
        // 날짜 기준 정렬
        return dailyStats.stream()
            .sorted((a, b) -> {
                String dateA = a.get("date").toString();
                String dateB = b.get("date").toString();
                return dateA.compareTo(dateB);
            })
            .limit(30) // 최대 30일
            .collect(Collectors.toList());
    }
    
    private List<Map<String, Object>> getProductSalesData(LocalDate startDate, LocalDate endDate) {
        return salesTransactionRepository.getProductSalesStatistics(startDate, endDate);
    }
    
    private List<Map<String, Object>> getTopMembersData(LocalDate startDate, LocalDate endDate) {
        return salesTransactionRepository.getTopMembers(startDate, endDate);
    }
    
    private Map<String, Object> getPaymentMethodDistribution(LocalDate startDate, LocalDate endDate) {
        var transactions = salesTransactionRepository.findByTransactionDateBetween(startDate, endDate);
        
        Map<String, Long> methodCounts = transactions.stream()
            .collect(Collectors.groupingBy(
                t -> t.getPaymentMethod() != null ? t.getPaymentMethod() : "UNKNOWN",
                Collectors.counting()
            ));
        
        Map<String, BigDecimal> methodAmounts = transactions.stream()
            .collect(Collectors.groupingBy(
                t -> t.getPaymentMethod() != null ? t.getPaymentMethod() : "UNKNOWN",
                Collectors.mapping(
                    t -> t.getFinalAmount(),
                    Collectors.reducing(BigDecimal.ZERO, BigDecimal::add)
                )
            ));
        
        List<Map<String, Object>> distribution = new ArrayList<>();
        for (String method : methodCounts.keySet()) {
            Map<String, Object> item = new HashMap<>();
            item.put("method", method);
            item.put("count", methodCounts.get(method));
            item.put("amount", methodAmounts.get(method));
            item.put("amountFormatted", formatCurrency(methodAmounts.get(method)));
            distribution.add(item);
        }
        
        // 금액 기준 정렬
        distribution.sort((a, b) -> {
            BigDecimal amountA = (BigDecimal) a.get("amount");
            BigDecimal amountB = (BigDecimal) b.get("amount");
            return amountB.compareTo(amountA);
        });
        
        return Map.of(
            "methods", distribution,
            "totalMethods", methodCounts.size()
        );
    }
    
    private String formatCurrency(BigDecimal amount) {
        if (amount == null) amount = BigDecimal.ZERO;
        return "₩" + String.format("%,.0f", amount);
    }
}