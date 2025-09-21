package com.dsai.rag.api.weather.controller;

import com.dsai.rag.api.weather.model.WeatherData;
import com.dsai.rag.api.weather.model.WeatherSlots;
import com.dsai.rag.api.weather.service.WeatherAgentService;
import com.dsai.rag.api.weather.service.WeatherService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 날씨 API 컨트롤러
 */
@RestController
@RequestMapping("/api/v1/weather")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Slf4j
@Tag(name = "Weather", description = "날씨 정보 API")
public class WeatherController {
    
    private final WeatherService weatherService;
    private final WeatherAgentService weatherAgentService;
    
    /**
     * 현재 날씨 조회
     */
    @GetMapping("/current")
    @Operation(summary = "현재 날씨 조회", description = "위치의 현재 날씨 정보를 조회합니다")
    public ResponseEntity<WeatherData> getCurrentWeather(
            @RequestParam(required = false) String place,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lon) {
        
        log.info("Get current weather: place={}, lat={}, lon={}", place, lat, lon);
        
        // 좌표 확인
        if (lat == null || lon == null) {
            if (place == null || place.isEmpty()) {
                return ResponseEntity.badRequest().build();
            }
            // 지오코딩 필요 (임시로 서울 좌표 사용)
            lat = 37.5665;
            lon = 126.9780;
            log.warn("Geocoding not implemented yet, using Seoul coordinates");
        }
        
        WeatherData weather = weatherService.getCurrentWeather(lat, lon);
        if (place != null) {
            weather.setPlace(place);
        }
        
        return ResponseEntity.ok(weather);
    }
    
    /**
     * 시간별 예보 조회
     */
    @GetMapping("/hourly")
    @Operation(summary = "시간별 예보 조회", description = "위치의 시간별 날씨 예보를 조회합니다")
    public ResponseEntity<WeatherData> getHourlyForecast(
            @RequestParam(required = false) String place,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lon,
            @RequestParam(defaultValue = "6") int hours) {
        
        log.info("Get hourly forecast: place={}, hours={}", place, hours);
        
        // 좌표 확인
        if (lat == null || lon == null) {
            if (place == null || place.isEmpty()) {
                return ResponseEntity.badRequest().build();
            }
            // 지오코딩 필요 (임시로 서울 좌표 사용)
            lat = 37.5665;
            lon = 126.9780;
        }
        
        WeatherData weather = weatherService.getHourlyForecast(lat, lon, hours);
        if (place != null) {
            weather.setPlace(place);
        }
        
        return ResponseEntity.ok(weather);
    }
    
    /**
     * 일별 예보 조회
     */
    @GetMapping("/daily")
    @Operation(summary = "일별 예보 조회", description = "위치의 일별 날씨 예보를 조회합니다")
    public ResponseEntity<WeatherData> getDailyForecast(
            @RequestParam(required = false) String place,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lon,
            @RequestParam(defaultValue = "3") int days) {
        
        log.info("Get daily forecast: place={}, days={}", place, days);
        
        // 좌표 확인
        if (lat == null || lon == null) {
            if (place == null || place.isEmpty()) {
                return ResponseEntity.badRequest().build();
            }
            // 지오코딩 필요 (임시로 서울 좌표 사용)
            lat = 37.5665;
            lon = 126.9780;
        }
        
        WeatherData weather = weatherService.getDailyForecast(lat, lon, days);
        if (place != null) {
            weather.setPlace(place);
        }
        
        return ResponseEntity.ok(weather);
    }
    
    /**
     * 자연어 날씨 조회 (챗봇)
     */
    @PostMapping("/chat")
    @Operation(summary = "자연어 날씨 조회", description = "자연어로 날씨를 조회합니다")
    public ResponseEntity<Map<String, Object>> chatWeather(@RequestBody Map<String, String> request) {
        String message = request.get("message");
        if (message == null || message.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        
        log.info("Chat weather request: {}", message);
        
        try {
            Map<String, Object> response = weatherAgentService.processMessage(message);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error processing chat message", e);
            return ResponseEntity.internalServerError()
                .body(Map.of(
                    "error", true,
                    "message", "날씨 정보를 처리하는 중 오류가 발생했습니다"
                ));
        }
    }
    
    /**
     * 캐시 통계 조회
     */
    @GetMapping("/cache/stats")
    @Operation(summary = "캐시 통계 조회", description = "날씨 캐시 통계를 조회합니다")
    public ResponseEntity<WeatherService.CacheStats> getCacheStats() {
        return ResponseEntity.ok(weatherService.getCacheStats());
    }
    
    /**
     * 캐시 초기화
     */
    @DeleteMapping("/cache")
    @Operation(summary = "캐시 초기화", description = "날씨 캐시를 초기화합니다")
    public ResponseEntity<Void> clearCache() {
        weatherService.clearCache();
        return ResponseEntity.noContent().build();
    }
}