package com.dsai.rag.api.controller;

import com.dsai.rag.api.service.SalesAnalyticsService;
import com.dsai.rag.common.dto.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/sales")
@RequiredArgsConstructor
@Tag(name = "Sales Analytics", description = "매출 분석 API")
@CrossOrigin(origins = "*")
public class SalesAnalyticsController {
    
    private final SalesAnalyticsService salesAnalyticsService;
    
    @GetMapping("/summary")
    @Operation(summary = "매출 요약 조회", description = "월별 매출 요약 데이터를 조회합니다")
    public ResponseEntity<BaseResponse<Map<String, Object>>> getSalesSummary() {
        log.info("Fetching sales summary");
        try {
            Map<String, Object> summary = salesAnalyticsService.getSalesSummary();
            return ResponseEntity.ok(BaseResponse.success(summary));
        } catch (Exception e) {
            log.error("Failed to fetch sales summary", e);
            return ResponseEntity.internalServerError()
                    .body(BaseResponse.error("FETCH_FAILED", e.getMessage()));
        }
    }
    
    @GetMapping("/daily")
    @Operation(summary = "일별 매출 조회", description = "일별 매출 데이터를 조회합니다")
    public ResponseEntity<BaseResponse<Map<String, Object>>> getDailySales() {
        log.info("Fetching daily sales");
        try {
            Map<String, Object> dailySales = salesAnalyticsService.getDailySales();
            return ResponseEntity.ok(BaseResponse.success(dailySales));
        } catch (Exception e) {
            log.error("Failed to fetch daily sales", e);
            return ResponseEntity.internalServerError()
                    .body(BaseResponse.error("FETCH_FAILED", e.getMessage()));
        }
    }
    
    @GetMapping("/products")
    @Operation(summary = "상품별 매출 조회", description = "상품별 매출 데이터를 조회합니다")
    public ResponseEntity<BaseResponse<Map<String, Object>>> getProductSales() {
        log.info("Fetching product sales");
        try {
            Map<String, Object> productSales = salesAnalyticsService.getProductSales();
            return ResponseEntity.ok(BaseResponse.success(productSales));
        } catch (Exception e) {
            log.error("Failed to fetch product sales", e);
            return ResponseEntity.internalServerError()
                    .body(BaseResponse.error("FETCH_FAILED", e.getMessage()));
        }
    }
    
    @GetMapping("/reload")
    @Operation(summary = "데이터 리로드", description = "Excel 파일에서 데이터를 다시 로드합니다")
    public ResponseEntity<BaseResponse<String>> reloadData() {
        log.info("Reloading sales data");
        try {
            salesAnalyticsService.reloadData();
            return ResponseEntity.ok(BaseResponse.success("Data reloaded successfully"));
        } catch (Exception e) {
            log.error("Failed to reload data", e);
            return ResponseEntity.internalServerError()
                    .body(BaseResponse.error("RELOAD_FAILED", e.getMessage()));
        }
    }
}