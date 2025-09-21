package com.dsai.rag.api.service;

import com.dsai.rag.api.entity.*;
import com.dsai.rag.api.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class MembershipImportService {
    
    private final MemberRepository memberRepository;
    private final MembershipTypeRepository membershipTypeRepository;
    private final MembershipPurchaseRepository membershipPurchaseRepository;
    
    // Excel 파일 헤더 확인
    public Map<String, Object> checkExcelHeaders(MultipartFile file) {
        Map<String, Object> result = new HashMap<>();
        
        try (InputStream is = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(is)) {
            
            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(0);
            
            List<String> headers = new ArrayList<>();
            if (headerRow != null) {
                for (Cell cell : headerRow) {
                    String header = getCellValueAsString(cell).trim();
                    if (!header.isEmpty()) {
                        headers.add(header);
                    }
                }
            }
            
            result.put("headers", headers);
            result.put("totalRows", sheet.getLastRowNum() + 1);
            
            // First data row sample
            Row dataRow = sheet.getRow(1);
            if (dataRow != null) {
                Map<String, String> firstRow = new HashMap<>();
                for (int i = 0; i < headers.size(); i++) {
                    Cell cell = dataRow.getCell(i);
                    firstRow.put(headers.get(i), getCellValueAsString(cell));
                }
                result.put("firstRow", firstRow);
            }
            
        } catch (IOException e) {
            result.put("error", e.getMessage());
        }
        
        return result;
    }
    
    // 고객정보.xlsx 파일 임포트
    @Transactional
    public Map<String, Object> importMembersFromExcel(MultipartFile file) {
        Map<String, Object> result = new HashMap<>();
        int successCount = 0;
        int failedCount = 0;
        int updatedCount = 0;
        List<String> errors = new ArrayList<>();
        
        try (InputStream is = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(is)) {
            
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null) {
                result.put("error", "엑셀 시트를 찾을 수 없습니다.");
                return result;
            }
            
            // 헤더 읽기
            Row headerRow = sheet.getRow(0);
            Map<String, Integer> headers = extractHeaders(headerRow);
            
            // 데이터 행 처리
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (isEmptyRow(row)) continue;
                
                try {
                    Member member = processMemberRow(row, headers);
                    if (member != null) {
                        // 기존 회원 체크
                        Optional<Member> existing = memberRepository.findByMemberCode(member.getMemberCode());
                        if (existing.isPresent()) {
                            updateExistingMember(existing.get(), member);
                            updatedCount++;
                        } else {
                            memberRepository.save(member);
                            successCount++;
                        }
                    }
                } catch (Exception e) {
                    failedCount++;
                    errors.add("행 " + (i + 1) + ": " + e.getMessage());
                    log.error("회원 데이터 처리 실패 - 행 {}: {}", i + 1, e.getMessage());
                }
            }
            
        } catch (IOException e) {
            log.error("파일 처리 실패", e);
            result.put("error", "파일 처리 중 오류: " + e.getMessage());
            return result;
        }
        
        result.put("success", true);
        result.put("successCount", successCount);
        result.put("updatedCount", updatedCount);
        result.put("failedCount", failedCount);
        result.put("errors", errors);
        result.put("message", String.format("신규 %d건, 수정 %d건, 실패 %d건", successCount, updatedCount, failedCount));
        
        return result;
    }
    
    // doubless.xlsx (회원권 구매정보) 파일 임포트
    @Transactional
    public Map<String, Object> importMembershipPurchasesFromExcel(MultipartFile file) {
        Map<String, Object> result = new HashMap<>();
        int successCount = 0;
        int failedCount = 0;
        List<String> errors = new ArrayList<>();
        
        try (InputStream is = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(is)) {
            
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null) {
                result.put("error", "엑셀 시트를 찾을 수 없습니다.");
                return result;
            }
            
            // 헤더 읽기
            Row headerRow = sheet.getRow(0);
            Map<String, Integer> headers = extractHeaders(headerRow);
            
            // 데이터 행 처리
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (isEmptyRow(row)) continue;
                
                try {
                    MembershipPurchase purchase = processPurchaseRow(row, headers);
                    if (purchase != null) {
                        membershipPurchaseRepository.save(purchase);
                        successCount++;
                    }
                } catch (Exception e) {
                    failedCount++;
                    errors.add("행 " + (i + 1) + ": " + e.getMessage());
                    log.error("회원권 구매 데이터 처리 실패 - 행 {}: {}", i + 1, e.getMessage());
                }
            }
            
        } catch (IOException e) {
            log.error("파일 처리 실패", e);
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
    
    private Map<String, Integer> extractHeaders(Row headerRow) {
        Map<String, Integer> headers = new HashMap<>();
        if (headerRow == null) return headers;
        
        for (Cell cell : headerRow) {
            String header = getCellValueAsString(cell).trim();
            if (!header.isEmpty()) {
                headers.put(header, cell.getColumnIndex());
            }
        }
        return headers;
    }
    
    private Member processMemberRow(Row row, Map<String, Integer> headers) {
        Member.MemberBuilder builder = Member.builder();
        
        // 회원코드
        String memberCode = getCellValue(row, headers, "회원번호", "회원코드", "번호");
        if (memberCode == null || memberCode.isEmpty()) {
            memberCode = "M" + System.currentTimeMillis() + new Random().nextInt(1000);
        }
        builder.memberCode(memberCode);
        
        // 회원명
        String memberName = getCellValue(row, headers, "회원명", "이름", "성명");
        if (memberName == null || memberName.isEmpty()) {
            throw new IllegalArgumentException("회원명이 없습니다.");
        }
        builder.memberName(memberName);
        
        // 전화번호
        builder.phoneNumber(getCellValue(row, headers, "전화번호", "연락처", "휴대폰"));
        
        // 이메일
        builder.email(getCellValue(row, headers, "이메일", "메일"));
        
        // 등록일
        LocalDate registrationDate = getDateCellValue(row, headers, "등록일", "가입일", "등록일자");
        builder.registrationDate(registrationDate != null ? registrationDate : LocalDate.now());
        
        // 상태
        String status = getCellValue(row, headers, "상태", "회원상태");
        builder.status(status != null ? status : "ACTIVE");
        
        return builder.build();
    }
    
    private MembershipPurchase processPurchaseRow(Row row, Map<String, Integer> headers) {
        MembershipPurchase.MembershipPurchaseBuilder builder = MembershipPurchase.builder();
        
        // 구매번호 생성
        String purchaseNo = "PUR-" + System.currentTimeMillis() + "-" + new Random().nextInt(1000);
        builder.purchaseNo(purchaseNo);
        
        // 회원 찾기 또는 생성
        String memberName = getCellValue(row, headers, "구매자", "회원명", "이름", "고객명");
        if (memberName == null || memberName.isEmpty()) {
            throw new IllegalArgumentException("회원명이 없습니다.");
        }
        
        Member member = memberRepository.findByMemberName(memberName)
            .orElseGet(() -> {
                Member newMember = Member.builder()
                    .memberCode("M" + System.currentTimeMillis())
                    .memberName(memberName)
                    .registrationDate(LocalDate.now())
                    .status("ACTIVE")
                    .build();
                return memberRepository.save(newMember);
            });
        builder.member(member);
        
        // 회원권 종류 찾기 또는 생성
        String typeName = getCellValue(row, headers, "상품명", "회원권", "회원권종류");
        if (typeName == null || typeName.isEmpty()) {
            throw new IllegalArgumentException("회원권 종류가 없습니다.");
        }
        
        MembershipType membershipType = findOrCreateMembershipType(typeName);
        builder.membershipType(membershipType);
        
        // 구매일 - 날짜 문자열 파싱 처리
        LocalDate purchaseDate = parseDateFromString(getCellValue(row, headers, "구매일", "결제일", "거래일"));
        builder.purchaseDate(purchaseDate != null ? purchaseDate : LocalDate.now());
        
        // 시작일/종료일
        LocalDate startDate = getDateCellValue(row, headers, "시작일", "개시일");
        LocalDate endDate = getDateCellValue(row, headers, "종료일", "만료일");
        
        if (startDate == null) {
            startDate = purchaseDate != null ? purchaseDate : LocalDate.now();
        }
        if (endDate == null && membershipType.getDurationMonths() != null) {
            endDate = startDate.plusMonths(membershipType.getDurationMonths());
        }
        
        builder.startDate(startDate);
        builder.endDate(endDate != null ? endDate : startDate.plusMonths(1));
        
        // 금액
        BigDecimal amount = getAmountCellValue(row, headers, "판매 금액", "금액", "결제금액", "가격");
        builder.originalPrice(amount != null ? amount : membershipType.getPrice());
        builder.finalPrice(amount != null ? amount : membershipType.getPrice());
        
        // 결제방법
        String paymentMethod = getCellValue(row, headers, "결제 수단", "결제방법", "결제수단");
        builder.paymentMethod(paymentMethod != null ? paymentMethod : "CASH");
        
        // PT/필라테스인 경우 잔여 횟수 설정
        if (membershipType.getSessionCount() != null) {
            Integer remainingSessions = getIntegerCellValue(row, headers, "잔여횟수", "남은횟수");
            builder.remainingSessions(remainingSessions != null ? remainingSessions : membershipType.getSessionCount());
        }
        
        // 상태
        String status = getCellValue(row, headers, "상태", "회원권상태");
        if (status != null) {
            try {
                builder.status(MembershipPurchase.PurchaseStatus.valueOf(status.toUpperCase()));
            } catch (IllegalArgumentException e) {
                builder.status(MembershipPurchase.PurchaseStatus.ACTIVE);
            }
        } else {
            builder.status(MembershipPurchase.PurchaseStatus.ACTIVE);
        }
        
        return builder.build();
    }
    
    private MembershipType findOrCreateMembershipType(String typeName) {
        return membershipTypeRepository.findByTypeName(typeName)
            .orElseGet(() -> {
                // 카테고리 추측
                MembershipType.MembershipCategory category = detectCategory(typeName);
                
                // 기간/횟수 추측
                Integer durationMonths = null;
                Integer sessionCount = null;
                
                if (category == MembershipType.MembershipCategory.PT || 
                    category == MembershipType.MembershipCategory.PILATES) {
                    // 횟수 추출 시도
                    sessionCount = extractNumber(typeName, "회", "횟");
                    if (sessionCount == null) sessionCount = 10; // 기본값
                } else {
                    // 기간 추출 시도
                    durationMonths = extractNumber(typeName, "개월", "달");
                    if (durationMonths == null) durationMonths = 1; // 기본값
                }
                
                MembershipType newType = MembershipType.builder()
                    .typeCode("TYPE-" + System.currentTimeMillis())
                    .typeName(typeName)
                    .category(category)
                    .durationMonths(durationMonths)
                    .sessionCount(sessionCount)
                    .price(BigDecimal.valueOf(100000)) // 기본 가격
                    .isActive(true)
                    .build();
                
                return membershipTypeRepository.save(newType);
            });
    }
    
    private MembershipType.MembershipCategory detectCategory(String typeName) {
        String lower = typeName.toLowerCase();
        if (lower.contains("pt") || lower.contains("퍼스널")) {
            return MembershipType.MembershipCategory.PT;
        } else if (lower.contains("필라") || lower.contains("pilates")) {
            return MembershipType.MembershipCategory.PILATES;
        } else if (lower.contains("요가") || lower.contains("yoga")) {
            return MembershipType.MembershipCategory.YOGA;
        } else if (lower.contains("수영") || lower.contains("swim")) {
            return MembershipType.MembershipCategory.SWIMMING;
        } else if (lower.contains("패키지") || lower.contains("package")) {
            return MembershipType.MembershipCategory.PACKAGE;
        }
        return MembershipType.MembershipCategory.GYM;
    }
    
    private Integer extractNumber(String text, String... keywords) {
        for (String keyword : keywords) {
            int index = text.indexOf(keyword);
            if (index > 0) {
                // 키워드 앞의 숫자 추출
                String beforeKeyword = text.substring(0, index).trim();
                String numbers = beforeKeyword.replaceAll("[^0-9]", "");
                if (!numbers.isEmpty()) {
                    try {
                        return Integer.parseInt(numbers);
                    } catch (NumberFormatException e) {
                        // ignore
                    }
                }
            }
        }
        return null;
    }
    
    private void updateExistingMember(Member existing, Member newData) {
        if (newData.getMemberName() != null) existing.setMemberName(newData.getMemberName());
        if (newData.getPhoneNumber() != null) existing.setPhoneNumber(newData.getPhoneNumber());
        if (newData.getEmail() != null) existing.setEmail(newData.getEmail());
        if (newData.getRegistrationDate() != null) existing.setRegistrationDate(newData.getRegistrationDate());
        if (newData.getStatus() != null) existing.setStatus(newData.getStatus());
    }
    
    private String getCellValue(Row row, Map<String, Integer> headers, String... possibleHeaders) {
        for (String header : possibleHeaders) {
            Integer index = headers.get(header);
            if (index != null) {
                Cell cell = row.getCell(index);
                if (cell != null) {
                    return getCellValueAsString(cell);
                }
            }
        }
        return null;
    }
    
    private LocalDate getDateCellValue(Row row, Map<String, Integer> headers, String... possibleHeaders) {
        for (String header : possibleHeaders) {
            Integer index = headers.get(header);
            if (index != null) {
                Cell cell = row.getCell(index);
                if (cell != null && cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toInstant()
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate();
                }
            }
        }
        return null;
    }
    
    private BigDecimal getAmountCellValue(Row row, Map<String, Integer> headers, String... possibleHeaders) {
        for (String header : possibleHeaders) {
            Integer index = headers.get(header);
            if (index != null) {
                Cell cell = row.getCell(index);
                if (cell != null) {
                    if (cell.getCellType() == CellType.NUMERIC) {
                        return BigDecimal.valueOf(cell.getNumericCellValue());
                    } else if (cell.getCellType() == CellType.STRING) {
                        String value = cell.getStringCellValue().trim();
                        // 콤마 제거 및 숫자만 추출
                        value = value.replaceAll("[,원]", "").trim();
                        try {
                            return new BigDecimal(value);
                        } catch (NumberFormatException e) {
                            log.warn("금액 파싱 실패: {}", value);
                        }
                    }
                }
            }
        }
        return null;
    }
    
    private Integer getIntegerCellValue(Row row, Map<String, Integer> headers, String... possibleHeaders) {
        for (String header : possibleHeaders) {
            Integer index = headers.get(header);
            if (index != null) {
                Cell cell = row.getCell(index);
                if (cell != null && cell.getCellType() == CellType.NUMERIC) {
                    return (int) cell.getNumericCellValue();
                }
            }
        }
        return null;
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
    
    private boolean isEmptyRow(Row row) {
        if (row == null) return true;
        
        for (Cell cell : row) {
            if (cell != null && !getCellValueAsString(cell).trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }
    
    private LocalDate parseDateFromString(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            return LocalDate.now();
        }
        
        // "2025-09-17 (수) 19:07:43" 형식 처리
        if (dateStr.contains("(")) {
            dateStr = dateStr.substring(0, dateStr.indexOf("(")).trim();
        }
        
        try {
            // "2025-09-17 19:07:43" 형식 처리
            if (dateStr.contains(" ") && dateStr.contains(":")) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                LocalDateTime dateTime = LocalDateTime.parse(dateStr, formatter);
                return dateTime.toLocalDate();
            }
            
            // "2025-09-17" 형식 처리
            return LocalDate.parse(dateStr);
        } catch (DateTimeParseException e) {
            log.warn("날짜 파싱 실패: {}, 현재 날짜로 설정", dateStr);
            return LocalDate.now();
        }
    }
}