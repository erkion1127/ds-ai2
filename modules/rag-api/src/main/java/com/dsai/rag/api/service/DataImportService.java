package com.dsai.rag.api.service;

import com.dsai.rag.api.entity.Member;
import com.dsai.rag.api.entity.Product;
import com.dsai.rag.api.entity.SalesTransaction;
import com.dsai.rag.api.repository.MemberRepository;
import com.dsai.rag.api.repository.ProductRepository;
import com.dsai.rag.api.repository.SalesTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class DataImportService {
    
    private final MemberRepository memberRepository;
    private final ProductRepository productRepository;
    private final SalesTransactionRepository salesTransactionRepository;
    
    // 날짜 포맷터들
    private static final DateTimeFormatter[] DATE_FORMATTERS = {
        DateTimeFormatter.ofPattern("yyyy-MM-dd"),
        DateTimeFormatter.ofPattern("yyyy.MM.dd"),
        DateTimeFormatter.ofPattern("yyyy/MM/dd"),
        DateTimeFormatter.ofPattern("yyyyMMdd"),
        DateTimeFormatter.ofPattern("M/d/yyyy"),
        DateTimeFormatter.ofPattern("dd-MM-yyyy")
    };
    
    // 엑셀 파일에서 데이터 임포트
    @Transactional
    public Map<String, Object> importExcelFile(MultipartFile file) {
        Map<String, Object> result = new HashMap<>();
        int successCount = 0;
        int failedCount = 0;
        List<String> errors = new ArrayList<>();
        
        if (file.isEmpty()) {
            result.put("error", "파일이 비어있습니다.");
            return result;
        }
        
        try (InputStream inputStream = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(inputStream)) {
            
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null) {
                result.put("error", "엑셀 시트를 찾을 수 없습니다.");
                return result;
            }
            
            // 헤더 행 읽기
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                result.put("error", "헤더 행을 찾을 수 없습니다.");
                return result;
            }
            
            // 헤더 인덱스 찾기
            Map<String, Integer> headerMap = new HashMap<>();
            for (Cell cell : headerRow) {
                String header = getCellValueAsString(cell).trim();
                headerMap.put(header, cell.getColumnIndex());
            }
            
            log.info("발견된 헤더: {}", headerMap.keySet());
            
            // 컬럼 인덱스 찾기
            Integer dateIndex = findColumnIndex(headerMap, "날짜", "일자", "Date", "거래일");
            Integer memberIndex = findColumnIndex(headerMap, "회원", "이름", "고객", "회원명", "Member");
            Integer productIndex = findColumnIndex(headerMap, "상품", "서비스", "품목", "상품명", "Product");
            Integer amountIndex = findColumnIndex(headerMap, "금액", "가격", "Amount", "Price", "매출");
            Integer paymentIndex = findColumnIndex(headerMap, "결제", "지불", "결제방법", "Payment");
            
            // 데이터 행 처리
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                
                try {
                    // 빈 행 체크
                    boolean isEmptyRow = true;
                    for (Cell cell : row) {
                        if (cell != null && !getCellValueAsString(cell).trim().isEmpty()) {
                            isEmptyRow = false;
                            break;
                        }
                    }
                    if (isEmptyRow) continue;
                    
                    // 데이터 추출
                    LocalDate transactionDate = dateIndex != null ? 
                        parseDateFromCell(row.getCell(dateIndex)) : LocalDate.now();
                    String memberName = memberIndex != null ? 
                        getCellValueAsString(row.getCell(memberIndex)) : null;
                    String productName = productIndex != null ? 
                        getCellValueAsString(row.getCell(productIndex)) : null;
                    BigDecimal amount = amountIndex != null ? 
                        parseAmountFromCell(row.getCell(amountIndex)) : BigDecimal.ZERO;
                    String paymentMethod = paymentIndex != null ? 
                        getCellValueAsString(row.getCell(paymentIndex)) : "UNKNOWN";
                    
                    // 트랜잭션 생성
                    SalesTransaction transaction = createTransactionFromExcel(
                        transactionDate, memberName, productName, amount, paymentMethod
                    );
                    
                    if (transaction != null) {
                        salesTransactionRepository.save(transaction);
                        successCount++;
                    }
                    
                } catch (Exception e) {
                    failedCount++;
                    errors.add("행 " + (i+1) + ": " + e.getMessage());
                    log.error("행 {} 처리 실패: {}", i+1, e.getMessage());
                }
            }
            
        } catch (Exception e) {
            log.error("엑셀 파일 임포트 실패", e);
            result.put("error", "파일 처리 중 오류: " + e.getMessage());
            return result;
        }
        
        result.put("success", true);
        result.put("successCount", successCount);
        result.put("failedCount", failedCount);
        result.put("errors", errors);
        result.put("message", String.format("%d건 성공, %d건 실패", successCount, failedCount));
        
        return result;
    }
    
    private String getCellValueAsString(Cell cell) {
        if (cell == null) return "";
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                }
                return String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return "";
        }
    }
    
    private LocalDate parseDateFromCell(Cell cell) {
        if (cell == null) return LocalDate.now();
        
        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            return cell.getDateCellValue().toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
        }
        
        String dateStr = getCellValueAsString(cell);
        return parseDate(dateStr);
    }
    
    private BigDecimal parseAmountFromCell(Cell cell) {
        if (cell == null) return BigDecimal.ZERO;
        
        if (cell.getCellType() == CellType.NUMERIC) {
            return BigDecimal.valueOf(cell.getNumericCellValue());
        }
        
        String amountStr = getCellValueAsString(cell);
        return parseAmount(amountStr);
    }
    
    private SalesTransaction createTransactionFromExcel(
            LocalDate transactionDate,
            String memberName,
            String productName,
            BigDecimal amount,
            String paymentMethod) {
        
        SalesTransaction transaction = new SalesTransaction();
        
        // 거래번호 생성
        transaction.setTransactionNo("TXN-" + System.currentTimeMillis() + "-" + new Random().nextInt(1000));
        transaction.setTransactionDate(transactionDate);
        
        // 회원 처리
        if (memberName != null && !memberName.trim().isEmpty()) {
            final LocalDate memberRegistrationDate = transactionDate;
            Member member = memberRepository.findByMemberName(memberName.trim())
                .orElseGet(() -> {
                    Member newMember = Member.builder()
                        .memberName(memberName.trim())
                        .memberCode("M" + System.currentTimeMillis())
                        .registrationDate(memberRegistrationDate)
                        .status("ACTIVE")
                        .build();
                    return memberRepository.save(newMember);
                });
            transaction.setMember(member);
        }
        
        // 상품 처리
        if (productName != null && !productName.trim().isEmpty()) {
            Product product = productRepository.findByProductName(productName.trim())
                .orElseGet(() -> {
                    Product newProduct = Product.builder()
                        .productName(productName.trim())
                        .productCode("P" + System.currentTimeMillis())
                        .productType(detectProductType(productName))
                        .status("ACTIVE")
                        .build();
                    return productRepository.save(newProduct);
                });
            transaction.setProduct(product);
        }
        
        transaction.setAmount(amount);
        transaction.setFinalAmount(amount);
        transaction.setPaymentMethod(paymentMethod);
        transaction.setPaymentStatus("COMPLETED");
        
        return transaction;
    }
    
    
    private Integer findColumnIndex(Map<String, Integer> headerMap, String... keywords) {
        for (String keyword : keywords) {
            for (Map.Entry<String, Integer> entry : headerMap.entrySet()) {
                if (entry.getKey().toLowerCase().contains(keyword.toLowerCase())) {
                    return entry.getValue();
                }
            }
        }
        return null;
    }
    
    
    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            return LocalDate.now();
        }
        
        dateStr = dateStr.trim();
        
        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                return LocalDate.parse(dateStr, formatter);
            } catch (Exception ignored) {
            }
        }
        
        // 날짜 파싱 실패 시 현재 날짜 반환
        log.warn("Failed to parse date: {}, using current date", dateStr);
        return LocalDate.now();
    }
    
    private BigDecimal parseAmount(String amountStr) {
        if (amountStr == null || amountStr.isEmpty()) {
            return BigDecimal.ZERO;
        }
        
        // 숫자가 아닌 문자 제거
        String cleanAmount = amountStr.replaceAll("[^0-9.-]", "");
        
        try {
            return new BigDecimal(cleanAmount);
        } catch (NumberFormatException e) {
            log.warn("Failed to parse amount: {}", amountStr);
            return BigDecimal.ZERO;
        }
    }
    
    private String detectProductType(String productName) {
        String lowerName = productName.toLowerCase();
        
        if (lowerName.contains("pt") || lowerName.contains("personal") || lowerName.contains("퍼스널")) {
            return "PT";
        } else if (lowerName.contains("필라") || lowerName.contains("pilates")) {
            return "PILATES";
        } else if (lowerName.contains("요가") || lowerName.contains("yoga")) {
            return "YOGA";
        } else if (lowerName.contains("헬스") || lowerName.contains("gym") || lowerName.contains("fitness")) {
            return "GYM";
        } else if (lowerName.contains("수영") || lowerName.contains("swim")) {
            return "SWIMMING";
        }
        
        return "GENERAL";
    }
}