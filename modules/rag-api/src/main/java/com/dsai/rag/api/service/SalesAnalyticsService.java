package com.dsai.rag.api.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.File;
import java.io.FileInputStream;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SalesAnalyticsService {
    
    private static final String EXCEL_FILE_PATH = "/Users/ijeongseob/IdeaProjects/ds-ai2/item/doubless/doubless.xlsx";
    private List<Map<String, Object>> salesData = new ArrayList<>();
    
    @PostConstruct
    public void init() {
        // 초기화 시에는 데이터를 로드하지 않음 - 데이터는 MembershipPurchaseRepository에서 관리
        log.info("SalesAnalyticsService initialized without loading Excel");
    }
    
    public void reloadData() {
        loadExcelData();
    }
    
    private void loadExcelData() {
        try {
            File file = new File(EXCEL_FILE_PATH);
            if (!file.exists()) {
                log.warn("Excel file not found: {}", EXCEL_FILE_PATH);
                return;
            }
            
            FileInputStream fis = new FileInputStream(file);
            Workbook workbook = new XSSFWorkbook(fis);
            Sheet sheet = workbook.getSheetAt(0);
            
            salesData.clear();
            
            // 헤더 읽기
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                log.warn("No header row found");
                workbook.close();
                fis.close();
                return;
            }
            
            List<String> headers = new ArrayList<>();
            for (Cell cell : headerRow) {
                headers.add(getCellValue(cell));
            }
            
            // 데이터 행 읽기
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                
                Map<String, Object> rowData = new HashMap<>();
                for (int j = 0; j < headers.size(); j++) {
                    Cell cell = row.getCell(j);
                    rowData.put(headers.get(j), getCellValue(cell));
                }
                
                if (!rowData.values().stream().allMatch(v -> v == null || v.toString().isEmpty())) {
                    salesData.add(rowData);
                }
            }
            
            workbook.close();
            fis.close();
            
            log.info("Loaded {} rows of sales data", salesData.size());
            
        } catch (Exception e) {
            log.error("Failed to load Excel data", e);
        }
    }
    
    private String getCellValue(Cell cell) {
        if (cell == null) return "";
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                    return sdf.format(cell.getDateCellValue());
                } else {
                    double value = cell.getNumericCellValue();
                    if (value == Math.floor(value)) {
                        return String.valueOf((long) value);
                    }
                    return String.valueOf(value);
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                try {
                    return String.valueOf(cell.getNumericCellValue());
                } catch (Exception e) {
                    return cell.getStringCellValue();
                }
            default:
                return "";
        }
    }
    
    public Map<String, Object> getSalesSummary() {
        Map<String, Object> summary = new HashMap<>();
        
        if (salesData.isEmpty()) {
            summary.put("totalRows", 0);
            summary.put("message", "No data available");
            return summary;
        }
        
        // 전체 행 수
        summary.put("totalRows", salesData.size());
        
        // 컬럼 정보
        summary.put("columns", salesData.get(0).keySet());
        
        // 매출 관련 컬럼 찾기 및 집계
        Map<String, Double> totals = new HashMap<>();
        for (String column : salesData.get(0).keySet()) {
            if (column.contains("매출") || column.contains("금액") || column.contains("수익")) {
                double total = salesData.stream()
                    .mapToDouble(row -> {
                        Object value = row.get(column);
                        if (value == null) return 0;
                        try {
                            return Double.parseDouble(value.toString().replaceAll("[^0-9.-]", ""));
                        } catch (Exception e) {
                            return 0;
                        }
                    })
                    .sum();
                totals.put(column, total);
            }
        }
        summary.put("totals", totals);
        
        // 샘플 데이터
        summary.put("sampleData", salesData.stream().limit(5).collect(Collectors.toList()));
        
        return summary;
    }
    
    public Map<String, Object> getDailySales() {
        Map<String, Object> result = new HashMap<>();
        
        // 날짜 컬럼 찾기
        String dateColumn = findDateColumn();
        if (dateColumn == null) {
            result.put("error", "No date column found");
            return result;
        }
        
        // 날짜별 집계
        Map<String, List<Map<String, Object>>> dailyData = salesData.stream()
            .collect(Collectors.groupingBy(row -> row.get(dateColumn).toString()));
        
        Map<String, Map<String, Object>> dailySummary = new HashMap<>();
        for (Map.Entry<String, List<Map<String, Object>>> entry : dailyData.entrySet()) {
            Map<String, Object> daySummary = new HashMap<>();
            daySummary.put("count", entry.getValue().size());
            
            // 매출 합계 계산
            for (String column : findAmountColumns()) {
                double total = entry.getValue().stream()
                    .mapToDouble(row -> parseDouble(row.get(column)))
                    .sum();
                daySummary.put(column + "_total", total);
            }
            
            dailySummary.put(entry.getKey(), daySummary);
        }
        
        result.put("dateColumn", dateColumn);
        result.put("dailyData", dailySummary);
        
        return result;
    }
    
    public Map<String, Object> getProductSales() {
        Map<String, Object> result = new HashMap<>();
        
        // 상품/서비스 관련 컬럼 찾기
        String productColumn = findProductColumn();
        if (productColumn == null) {
            result.put("message", "No product column found, showing all data");
            result.put("data", salesData);
            return result;
        }
        
        // 상품별 집계
        Map<String, List<Map<String, Object>>> productData = salesData.stream()
            .filter(row -> row.get(productColumn) != null && !row.get(productColumn).toString().isEmpty())
            .collect(Collectors.groupingBy(row -> row.get(productColumn).toString()));
        
        Map<String, Map<String, Object>> productSummary = new HashMap<>();
        for (Map.Entry<String, List<Map<String, Object>>> entry : productData.entrySet()) {
            Map<String, Object> summary = new HashMap<>();
            summary.put("count", entry.getValue().size());
            
            // 금액 합계
            for (String column : findAmountColumns()) {
                double total = entry.getValue().stream()
                    .mapToDouble(row -> parseDouble(row.get(column)))
                    .sum();
                summary.put(column + "_total", total);
            }
            
            productSummary.put(entry.getKey(), summary);
        }
        
        result.put("productColumn", productColumn);
        result.put("productData", productSummary);
        
        return result;
    }
    
    private String findDateColumn() {
        if (salesData.isEmpty()) return null;
        
        for (String column : salesData.get(0).keySet()) {
            if (column.contains("날짜") || column.contains("일자") || column.contains("date") || 
                column.contains("Date") || column.contains("일시")) {
                return column;
            }
        }
        return null;
    }
    
    private String findProductColumn() {
        if (salesData.isEmpty()) return null;
        
        for (String column : salesData.get(0).keySet()) {
            if (column.contains("상품") || column.contains("품목") || column.contains("서비스") || 
                column.contains("이름") || column.contains("회원")) {
                return column;
            }
        }
        return null;
    }
    
    private List<String> findAmountColumns() {
        List<String> amountColumns = new ArrayList<>();
        if (salesData.isEmpty()) return amountColumns;
        
        for (String column : salesData.get(0).keySet()) {
            if (column.contains("금액") || column.contains("매출") || column.contains("수익") || 
                column.contains("가격") || column.contains("비용")) {
                amountColumns.add(column);
            }
        }
        return amountColumns;
    }
    
    private double parseDouble(Object value) {
        if (value == null) return 0;
        try {
            return Double.parseDouble(value.toString().replaceAll("[^0-9.-]", ""));
        } catch (Exception e) {
            return 0;
        }
    }
}