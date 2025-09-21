package com.dsai.rag.api.controller;

import com.dsai.rag.api.service.DataImportService;
import com.dsai.rag.api.service.DoublessDashboardService;
import com.dsai.rag.common.dto.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/doubless")
@RequiredArgsConstructor
@Tag(name = "Doubless Dashboard", description = "더블리스 헬스장 매출 대시보드 API")
@CrossOrigin(origins = "*")
public class DoublessDashboardController {
    
    private final DoublessDashboardService dashboardService;
    private final DataImportService dataImportService;
    
    @PostMapping("/import")
    @Operation(summary = "엑셀 파일 임포트", description = "엑셀 파일(.xlsx)에서 데이터를 임포트합니다")
    public ResponseEntity<BaseResponse<Map<String, Object>>> importExcelData(
            @RequestParam("file") MultipartFile file) {
        log.info("Starting Excel file import: {}", file.getOriginalFilename());
        
        try {
            // 파일 확장자 검증
            String filename = file.getOriginalFilename();
            if (filename == null || (!filename.endsWith(".xlsx") && !filename.endsWith(".xls"))) {
                return ResponseEntity.badRequest()
                    .body(BaseResponse.error("INVALID_FILE", "엑셀 파일(.xlsx, .xls)만 업로드 가능합니다."));
            }
            
            Map<String, Object> result = dataImportService.importExcelFile(file);
            
            if (result.containsKey("error")) {
                return ResponseEntity.badRequest()
                    .body(BaseResponse.error("IMPORT_FAILED", result.get("error").toString()));
            }
            
            log.info("Import completed: {}", result.get("message"));
            return ResponseEntity.ok(BaseResponse.success(result));
            
        } catch (Exception e) {
            log.error("Excel import failed", e);
            return ResponseEntity.internalServerError()
                .body(BaseResponse.error("IMPORT_ERROR", "파일 처리 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }
    
    @GetMapping("/dashboard")
    @Operation(summary = "대시보드 데이터 조회", description = "대시보드에 표시할 종합 데이터를 조회합니다")
    public ResponseEntity<BaseResponse<Map<String, Object>>> getDashboardData(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        if (startDate == null) {
            startDate = LocalDate.now().minusDays(30); // 기본값: 최근 30일
        }
        if (endDate == null) {
            endDate = LocalDate.now();
        }
        
        log.info("Fetching dashboard data from {} to {}", startDate, endDate);
        
        try {
            Map<String, Object> dashboardData = dashboardService.getDashboardData(startDate, endDate);
            return ResponseEntity.ok(BaseResponse.success(dashboardData));
        } catch (Exception e) {
            log.error("Failed to fetch dashboard data", e);
            return ResponseEntity.internalServerError()
                .body(BaseResponse.error("FETCH_ERROR", e.getMessage()));
        }
    }
    
    @GetMapping("/sales/daily")
    @Operation(summary = "일별 매출 조회", description = "기간별 일일 매출 통계를 조회합니다")
    public ResponseEntity<BaseResponse<Object>> getDailySales(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        if (startDate == null) {
            startDate = LocalDate.now().minusDays(30);
        }
        if (endDate == null) {
            endDate = LocalDate.now();
        }
        
        try {
            return ResponseEntity.ok(BaseResponse.success(
                dashboardService.getDailySales(startDate, endDate)));
        } catch (Exception e) {
            log.error("Failed to fetch daily sales", e);
            return ResponseEntity.internalServerError()
                .body(BaseResponse.error("FETCH_ERROR", e.getMessage()));
        }
    }
    
    @GetMapping("/sales/product")
    @Operation(summary = "상품별 매출 조회", description = "상품별 매출 통계를 조회합니다")
    public ResponseEntity<BaseResponse<Object>> getProductSales(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        if (startDate == null) {
            startDate = LocalDate.now().minusDays(30);
        }
        if (endDate == null) {
            endDate = LocalDate.now();
        }
        
        try {
            return ResponseEntity.ok(BaseResponse.success(
                dashboardService.getProductSales(startDate, endDate)));
        } catch (Exception e) {
            log.error("Failed to fetch product sales", e);
            return ResponseEntity.internalServerError()
                .body(BaseResponse.error("FETCH_ERROR", e.getMessage()));
        }
    }
    
    @GetMapping("/members/top")
    @Operation(summary = "상위 회원 조회", description = "매출 기여도가 높은 상위 회원을 조회합니다")
    public ResponseEntity<BaseResponse<Object>> getTopMembers(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        if (startDate == null) {
            startDate = LocalDate.now().minusDays(30);
        }
        if (endDate == null) {
            endDate = LocalDate.now();
        }
        
        try {
            return ResponseEntity.ok(BaseResponse.success(
                dashboardService.getTopMembers(startDate, endDate)));
        } catch (Exception e) {
            log.error("Failed to fetch top members", e);
            return ResponseEntity.internalServerError()
                .body(BaseResponse.error("FETCH_ERROR", e.getMessage()));
        }
    }
    
    @GetMapping("/transactions/recent")
    @Operation(summary = "최근 거래 조회", description = "최근 거래 내역을 조회합니다")
    public ResponseEntity<BaseResponse<Object>> getRecentTransactions(
            @RequestParam(defaultValue = "20") int limit) {
        
        try {
            return ResponseEntity.ok(BaseResponse.success(
                dashboardService.getRecentTransactions(limit)));
        } catch (Exception e) {
            log.error("Failed to fetch recent transactions", e);
            return ResponseEntity.internalServerError()
                .body(BaseResponse.error("FETCH_ERROR", e.getMessage()));
        }
    }
}