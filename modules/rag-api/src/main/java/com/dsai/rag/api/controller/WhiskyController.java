package com.dsai.rag.api.controller;

import com.dsai.rag.api.agent.WhiskyAgent;
import com.dsai.rag.api.entity.Whisky;
import com.dsai.rag.api.repository.WhiskyRepository;
import com.dsai.rag.common.dto.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/whisky")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "Whisky", description = "위스키 추천 및 정보 API")
public class WhiskyController {

    private final WhiskyAgent whiskyAgent;
    private final WhiskyRepository whiskyRepository;

    @PostMapping("/chat")
    @Operation(summary = "위스키 관련 대화", description = "AI 소믈리에와 위스키 관련 대화")
    public ResponseEntity<BaseResponse<Map<String, Object>>> chatAboutWhisky(
            @RequestBody Map<String, String> request) {

        String query = request.get("query");
        if (query == null || query.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(BaseResponse.error("INVALID_REQUEST", "질문을 입력해주세요."));
        }

        try {
            Map<String, Object> response = whiskyAgent.processWhiskyQuery(query);
            return ResponseEntity.ok(BaseResponse.success(response));
        } catch (Exception e) {
            log.error("Error in whisky chat", e);
            return ResponseEntity.internalServerError()
                    .body(BaseResponse.error("CHAT_ERROR", "처리 중 오류가 발생했습니다."));
        }
    }

    @GetMapping("/recommend/type/{type}")
    @Operation(summary = "타입별 위스키 추천", description = "Single Malt, Blended, Bourbon 등 타입별 추천")
    public ResponseEntity<BaseResponse<String>> recommendByType(@PathVariable String type) {
        try {
            String recommendations = whiskyAgent.recommendByType(type);
            return ResponseEntity.ok(BaseResponse.success(recommendations));
        } catch (Exception e) {
            log.error("Error recommending by type", e);
            return ResponseEntity.internalServerError()
                    .body(BaseResponse.error("RECOMMEND_ERROR", e.getMessage()));
        }
    }

    @GetMapping("/recommend/region/{region}")
    @Operation(summary = "지역별 위스키 추천", description = "Scotland, Ireland, USA, Japan 등 지역별 추천")
    public ResponseEntity<BaseResponse<String>> recommendByRegion(@PathVariable String region) {
        try {
            String recommendations = whiskyAgent.recommendByRegion(region);
            return ResponseEntity.ok(BaseResponse.success(recommendations));
        } catch (Exception e) {
            log.error("Error recommending by region", e);
            return ResponseEntity.internalServerError()
                    .body(BaseResponse.error("RECOMMEND_ERROR", e.getMessage()));
        }
    }

    @GetMapping("/recommend/flavor/{flavor}")
    @Operation(summary = "맛 프로필별 위스키 추천", description = "peaty, smoky, sweet 등 맛별 추천")
    public ResponseEntity<BaseResponse<String>> recommendByFlavor(@PathVariable String flavor) {
        try {
            String recommendations = whiskyAgent.recommendByFlavor(flavor);
            return ResponseEntity.ok(BaseResponse.success(recommendations));
        } catch (Exception e) {
            log.error("Error recommending by flavor", e);
            return ResponseEntity.internalServerError()
                    .body(BaseResponse.error("RECOMMEND_ERROR", e.getMessage()));
        }
    }

    @GetMapping("/recommend/price/{range}")
    @Operation(summary = "가격대별 위스키 추천", description = "1-5 가격대별 추천 (1:저가, 5:프리미엄)")
    public ResponseEntity<BaseResponse<String>> recommendByPrice(@PathVariable int range) {
        try {
            String recommendations = whiskyAgent.recommendByPriceRange(range);
            return ResponseEntity.ok(BaseResponse.success(recommendations));
        } catch (Exception e) {
            log.error("Error recommending by price", e);
            return ResponseEntity.internalServerError()
                    .body(BaseResponse.error("RECOMMEND_ERROR", e.getMessage()));
        }
    }

    @GetMapping("/recommend/occasion/{occasion}")
    @Operation(summary = "상황별 위스키 추천", description = "입문용, 선물용, 특별한날 등 상황별 추천")
    public ResponseEntity<BaseResponse<String>> recommendByOccasion(@PathVariable String occasion) {
        try {
            String recommendations = whiskyAgent.recommendByOccasion(occasion);
            return ResponseEntity.ok(BaseResponse.success(recommendations));
        } catch (Exception e) {
            log.error("Error recommending by occasion", e);
            return ResponseEntity.internalServerError()
                    .body(BaseResponse.error("RECOMMEND_ERROR", e.getMessage()));
        }
    }

    @GetMapping("/top")
    @Operation(summary = "인기 위스키 TOP 10", description = "평점 기준 상위 10개 위스키")
    public ResponseEntity<BaseResponse<String>> getTopWhiskies() {
        try {
            String topWhiskies = whiskyAgent.getTopRatedWhiskies();
            return ResponseEntity.ok(BaseResponse.success(topWhiskies));
        } catch (Exception e) {
            log.error("Error getting top whiskies", e);
            return ResponseEntity.internalServerError()
                    .body(BaseResponse.error("TOP_ERROR", e.getMessage()));
        }
    }

    @GetMapping("/details/{name}")
    @Operation(summary = "위스키 상세 정보", description = "특정 위스키의 상세 정보 조회")
    public ResponseEntity<BaseResponse<String>> getWhiskyDetails(@PathVariable String name) {
        try {
            String details = whiskyAgent.getWhiskyDetails(name);
            return ResponseEntity.ok(BaseResponse.success(details));
        } catch (Exception e) {
            log.error("Error getting whisky details", e);
            return ResponseEntity.internalServerError()
                    .body(BaseResponse.error("DETAIL_ERROR", e.getMessage()));
        }
    }

    @GetMapping("/search")
    @Operation(summary = "위스키 검색", description = "키워드로 위스키 검색")
    public ResponseEntity<BaseResponse<List<Whisky>>> searchWhiskies(
            @RequestParam String keyword) {
        try {
            List<Whisky> whiskies = whiskyRepository.searchWhiskies(keyword);
            return ResponseEntity.ok(BaseResponse.success(whiskies));
        } catch (Exception e) {
            log.error("Error searching whiskies", e);
            return ResponseEntity.internalServerError()
                    .body(BaseResponse.error("SEARCH_ERROR", e.getMessage()));
        }
    }

    @PostMapping("/add")
    @Operation(summary = "위스키 추가", description = "새로운 위스키 정보 추가")
    public ResponseEntity<BaseResponse<Whisky>> addWhisky(@RequestBody Whisky whisky) {
        try {
            Whisky saved = whiskyRepository.save(whisky);
            return ResponseEntity.ok(BaseResponse.success(saved));
        } catch (Exception e) {
            log.error("Error adding whisky", e);
            return ResponseEntity.internalServerError()
                    .body(BaseResponse.error("ADD_ERROR", e.getMessage()));
        }
    }

    @GetMapping("/all")
    @Operation(summary = "전체 위스키 목록", description = "모든 위스키 목록 조회")
    public ResponseEntity<BaseResponse<List<Whisky>>> getAllWhiskies() {
        try {
            List<Whisky> whiskies = whiskyRepository.findByIsAvailableTrueOrderByRatingDesc();
            return ResponseEntity.ok(BaseResponse.success(whiskies));
        } catch (Exception e) {
            log.error("Error getting all whiskies", e);
            return ResponseEntity.internalServerError()
                    .body(BaseResponse.error("LIST_ERROR", e.getMessage()));
        }
    }
}