package com.dsai.rag.api.controller;

import com.dsai.rag.api.entity.Member;
import com.dsai.rag.api.entity.MembershipPurchase;
import com.dsai.rag.api.entity.MembershipType;
import com.dsai.rag.api.repository.MemberRepository;
import com.dsai.rag.api.repository.MembershipPurchaseRepository;
import com.dsai.rag.api.repository.MembershipTypeRepository;
import com.dsai.rag.api.service.MembershipImportService;
import com.dsai.rag.common.dto.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/membership")
@RequiredArgsConstructor
@Tag(name = "Membership Management", description = "헬스장 회원권 관리 API")
@CrossOrigin(origins = "*")
public class MembershipController {
    
    private final MemberRepository memberRepository;
    private final MembershipTypeRepository membershipTypeRepository;
    private final MembershipPurchaseRepository membershipPurchaseRepository;
    private final MembershipImportService membershipImportService;
    
    // ========== 회원 관리 ==========
    
    @GetMapping("/members")
    @Operation(summary = "회원 목록 조회", description = "전체 회원 목록을 조회합니다")
    public ResponseEntity<BaseResponse<Page<Member>>> getMembers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status) {
        
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("registrationDate").descending());
        Page<Member> members;
        
        if (status != null && !status.isEmpty()) {
            members = memberRepository.findByStatus(status, pageRequest);
        } else {
            members = memberRepository.findAll(pageRequest);
        }
        
        return ResponseEntity.ok(BaseResponse.success(members));
    }
    
    @GetMapping("/members/{id}")
    @Operation(summary = "회원 상세 조회", description = "특정 회원의 상세 정보를 조회합니다")
    public ResponseEntity<BaseResponse<Member>> getMember(@PathVariable Long id) {
        return memberRepository.findById(id)
            .map(member -> ResponseEntity.ok(BaseResponse.success(member)))
            .orElse(ResponseEntity.notFound().build());
    }
    
    @PostMapping("/members/import")
    @Operation(summary = "회원 정보 엑셀 임포트", description = "고객정보.xlsx 파일을 임포트합니다")
    public ResponseEntity<BaseResponse<Map<String, Object>>> importMembers(
            @RequestParam("file") MultipartFile file) {
        
        try {
            if (!isExcelFile(file)) {
                return ResponseEntity.badRequest()
                    .body(BaseResponse.error("INVALID_FILE", "엑셀 파일(.xlsx)만 업로드 가능합니다."));
            }
            
            Map<String, Object> result = membershipImportService.importMembersFromExcel(file);
            
            if (result.containsKey("error")) {
                return ResponseEntity.badRequest()
                    .body(BaseResponse.error("IMPORT_FAILED", result.get("error").toString()));
            }
            
            return ResponseEntity.ok(BaseResponse.success(result));
            
        } catch (Exception e) {
            log.error("회원 정보 임포트 실패", e);
            return ResponseEntity.internalServerError()
                .body(BaseResponse.error("IMPORT_ERROR", "파일 처리 중 오류가 발생했습니다."));
        }
    }
    
    // ========== 회원권 종류 관리 ==========
    
    @GetMapping("/types")
    @Operation(summary = "회원권 종류 목록", description = "판매 중인 회원권 종류를 조회합니다")
    public ResponseEntity<BaseResponse<List<MembershipType>>> getMembershipTypes(
            @RequestParam(required = false) String category) {
        
        List<MembershipType> types;
        if (category != null && !category.isEmpty()) {
            types = membershipTypeRepository.findByCategoryAndIsActiveTrue(
                MembershipType.MembershipCategory.valueOf(category.toUpperCase())
            );
        } else {
            types = membershipTypeRepository.findByIsActiveTrue();
        }
        
        return ResponseEntity.ok(BaseResponse.success(types));
    }
    
    @PostMapping("/types")
    @Operation(summary = "회원권 종류 등록", description = "새로운 회원권 종류를 등록합니다")
    public ResponseEntity<BaseResponse<MembershipType>> createMembershipType(
            @RequestBody MembershipType membershipType) {
        
        try {
            MembershipType saved = membershipTypeRepository.save(membershipType);
            return ResponseEntity.ok(BaseResponse.success(saved));
        } catch (Exception e) {
            log.error("회원권 종류 등록 실패", e);
            return ResponseEntity.internalServerError()
                .body(BaseResponse.error("CREATE_FAILED", e.getMessage()));
        }
    }
    
    // ========== 회원권 구매 관리 ==========
    
    @GetMapping("/purchases")
    @Operation(summary = "회원권 구매 목록", description = "회원권 구매 내역을 조회합니다")
    public ResponseEntity<BaseResponse<Page<MembershipPurchase>>> getPurchases(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long memberId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("purchaseDate").descending());
        Page<MembershipPurchase> purchases;
        
        if (memberId != null) {
            if (status != null) {
                purchases = membershipPurchaseRepository.findByMemberIdAndStatus(
                    memberId, MembershipPurchase.PurchaseStatus.valueOf(status.toUpperCase()), pageRequest);
            } else {
                purchases = membershipPurchaseRepository.findByMemberId(memberId, pageRequest);
            }
        } else {
            purchases = membershipPurchaseRepository.findAll(pageRequest);
        }
        
        return ResponseEntity.ok(BaseResponse.success(purchases));
    }
    
    @PostMapping("/purchases/import")
    @Operation(summary = "회원권 구매 정보 엑셀 임포트", description = "doubless.xlsx 파일을 임포트합니다")
    public ResponseEntity<BaseResponse<Map<String, Object>>> importPurchases(
            @RequestParam("file") MultipartFile file) {
        
        try {
            if (!isExcelFile(file)) {
                return ResponseEntity.badRequest()
                    .body(BaseResponse.error("INVALID_FILE", "엑셀 파일(.xlsx)만 업로드 가능합니다."));
            }
            
            Map<String, Object> result = membershipImportService.importMembershipPurchasesFromExcel(file);
            
            if (result.containsKey("error")) {
                return ResponseEntity.badRequest()
                    .body(BaseResponse.error("IMPORT_FAILED", result.get("error").toString()));
            }
            
            return ResponseEntity.ok(BaseResponse.success(result));
            
        } catch (Exception e) {
            log.error("회원권 구매 정보 임포트 실패", e);
            return ResponseEntity.internalServerError()
                .body(BaseResponse.error("IMPORT_ERROR", "파일 처리 중 오류가 발생했습니다."));
        }
    }
    
    // ========== 통계 및 분석 ==========
    
    @GetMapping("/statistics/expiring")
    @Operation(summary = "만료 예정 회원권", description = "30일 이내 만료 예정인 회원권을 조회합니다")
    public ResponseEntity<BaseResponse<List<MembershipPurchase>>> getExpiringMemberships() {
        LocalDate today = LocalDate.now();
        LocalDate thirtyDaysLater = today.plusDays(30);
        
        List<MembershipPurchase> expiring = membershipPurchaseRepository
            .findExpiringMemberships(today, thirtyDaysLater);
        
        return ResponseEntity.ok(BaseResponse.success(expiring));
    }
    
    @GetMapping("/statistics/monthly")
    @Operation(summary = "월별 매출 통계", description = "월별 회원권 판매 통계를 조회합니다")
    public ResponseEntity<BaseResponse<List<Map<String, Object>>>> getMonthlySalesStatistics(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        List<Map<String, Object>> statistics = membershipPurchaseRepository
            .getMonthlySalesStatistics(startDate, endDate);
        
        return ResponseEntity.ok(BaseResponse.success(statistics));
    }
    
    @GetMapping("/statistics/by-type")
    @Operation(summary = "회원권 종류별 매출", description = "회원권 종류별 판매 통계를 조회합니다")
    public ResponseEntity<BaseResponse<List<Map<String, Object>>>> getMembershipTypeSalesStatistics(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        List<Map<String, Object>> statistics = membershipPurchaseRepository
            .getMembershipTypeSalesStatistics(startDate, endDate);
        
        return ResponseEntity.ok(BaseResponse.success(statistics));
    }
    
    @GetMapping("/members/{memberId}/active")
    @Operation(summary = "회원 활성 회원권", description = "특정 회원의 활성 회원권을 조회합니다")
    public ResponseEntity<BaseResponse<List<MembershipPurchase>>> getActiveMemberships(
            @PathVariable Long memberId) {
        
        List<MembershipPurchase> activeMemberships = membershipPurchaseRepository
            .findActiveMemberships(memberId, LocalDate.now());
        
        return ResponseEntity.ok(BaseResponse.success(activeMemberships));
    }
    
    @GetMapping("/dashboard")
    @Operation(summary = "대시보드 요약", description = "회원권 관리 대시보드 요약 데이터를 조회합니다")
    public ResponseEntity<BaseResponse<Map<String, Object>>> getDashboardSummary() {
        Map<String, Object> dashboard = new HashMap<>();
        
        // 전체 회원 수
        dashboard.put("totalMembers", memberRepository.count());
        dashboard.put("activeMembers", memberRepository.countByStatus("ACTIVE"));
        
        // 활성 회원권 수
        dashboard.put("activeMemberships", 
            membershipPurchaseRepository.countByStatus(MembershipPurchase.PurchaseStatus.ACTIVE));
        
        // 만료 예정 회원권 (7일 이내)
        LocalDate today = LocalDate.now();
        LocalDate sevenDaysLater = today.plusDays(7);
        dashboard.put("expiringInSevenDays", 
            membershipPurchaseRepository.findExpiringMemberships(today, sevenDaysLater).size());
        
        // 회원권 종류 수
        dashboard.put("membershipTypes", membershipTypeRepository.countByIsActiveTrue());
        
        return ResponseEntity.ok(BaseResponse.success(dashboard));
    }
    
    @PostMapping("/debug/excel-headers")
    @Operation(summary = "Excel 파일 헤더 확인", description = "Excel 파일의 헤더를 확인합니다")
    public ResponseEntity<BaseResponse<Map<String, Object>>> checkExcelHeaders(
            @RequestParam("file") MultipartFile file) {
        
        Map<String, Object> result = membershipImportService.checkExcelHeaders(file);
        return ResponseEntity.ok(BaseResponse.success(result));
    }
    
    private boolean isExcelFile(MultipartFile file) {
        String filename = file.getOriginalFilename();
        return filename != null && (filename.endsWith(".xlsx") || filename.endsWith(".xls"));
    }
}