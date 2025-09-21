package com.dsai.rag.api.weather.service;

import com.dsai.rag.api.weather.model.WeatherData;
import com.dsai.rag.api.weather.model.WeatherSlots;
import com.dsai.rag.api.weather.provider.WeatherProvider;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * 날씨 서비스
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class WeatherService {
    
    private final List<WeatherProvider> weatherProviders;
    private Cache<String, WeatherData> weatherCache;
    
    @Value("${weather.cache.ttl-seconds:600}")
    private int cacheTtlSeconds;
    
    @Value("${weather.cache.max-size:1000}")
    private int cacheMaxSize;
    
    @PostConstruct
    public void init() {
        // Caffeine 캐시 초기화
        weatherCache = Caffeine.newBuilder()
            .maximumSize(cacheMaxSize)
            .expireAfterWrite(Duration.ofSeconds(cacheTtlSeconds))
            .recordStats()
            .build();
        
        log.info("WeatherService initialized with {} providers", weatherProviders.size());
        weatherProviders.forEach(provider -> 
            log.info("  - {}: {}", provider.getName(), provider.isAvailable() ? "Available" : "Not configured")
        );
    }
    
    /**
     * 현재 날씨 조회
     */
    public WeatherData getCurrentWeather(double lat, double lon) {
        String cacheKey = generateCacheKey(lat, lon, "current", 0, 0);
        
        return weatherCache.get(cacheKey, key -> {
            WeatherProvider provider = selectProvider();
            if (provider == null) {
                throw new RuntimeException("No weather provider available");
            }
            
            try {
                log.info("Fetching current weather from {} for lat={}, lon={}", 
                    provider.getName(), lat, lon);
                return provider.getWeatherData(lat, lon, true, 0, 0);
            } catch (Exception e) {
                log.error("Failed to fetch weather from {}", provider.getName(), e);
                // Fallback to next provider if available
                WeatherProvider fallback = selectFallbackProvider(provider);
                if (fallback != null) {
                    try {
                        log.info("Trying fallback provider: {}", fallback.getName());
                        return fallback.getWeatherData(lat, lon, true, 0, 0);
                    } catch (Exception fe) {
                        log.error("Fallback provider also failed", fe);
                    }
                }
                throw new RuntimeException("Failed to fetch weather data", e);
            }
        });
    }
    
    /**
     * 시간별 예보 조회
     */
    public WeatherData getHourlyForecast(double lat, double lon, int hours) {
        String cacheKey = generateCacheKey(lat, lon, "hourly", hours, 0);
        
        return weatherCache.get(cacheKey, key -> {
            WeatherProvider provider = selectProvider();
            if (provider == null) {
                throw new RuntimeException("No weather provider available");
            }
            
            try {
                log.info("Fetching hourly forecast from {} for {} hours", provider.getName(), hours);
                return provider.getWeatherData(lat, lon, false, hours, 0);
            } catch (Exception e) {
                log.error("Failed to fetch hourly forecast", e);
                throw new RuntimeException("Failed to fetch hourly forecast", e);
            }
        });
    }
    
    /**
     * 일별 예보 조회
     */
    public WeatherData getDailyForecast(double lat, double lon, int days) {
        String cacheKey = generateCacheKey(lat, lon, "daily", 0, days);
        
        return weatherCache.get(cacheKey, key -> {
            WeatherProvider provider = selectProvider();
            if (provider == null) {
                throw new RuntimeException("No weather provider available");
            }
            
            try {
                log.info("Fetching daily forecast from {} for {} days", provider.getName(), days);
                return provider.getWeatherData(lat, lon, false, 0, days);
            } catch (Exception e) {
                log.error("Failed to fetch daily forecast", e);
                throw new RuntimeException("Failed to fetch daily forecast", e);
            }
        });
    }
    
    /**
     * 통합 날씨 데이터 조회
     */
    public WeatherData getWeatherData(WeatherSlots slots) {
        if (slots.getLat() == null || slots.getLon() == null) {
            throw new IllegalArgumentException("Latitude and longitude are required");
        }
        
        double lat = slots.getLat();
        double lon = slots.getLon();
        
        switch (slots.getIntent()) {
            case CURRENT:
                return getCurrentWeather(lat, lon);
            case HOURLY:
                return getHourlyForecast(lat, lon, slots.getHours());
            case DAILY:
                return getDailyForecast(lat, lon, slots.getDays());
            default:
                // 기본: 현재 날씨 + 6시간 예보 + 3일 예보
                return getCompleteWeatherData(lat, lon, slots.getHours(), slots.getDays());
        }
    }
    
    /**
     * 완전한 날씨 데이터 조회 (현재 + 시간별 + 일별)
     */
    public WeatherData getCompleteWeatherData(double lat, double lon, int hours, int days) {
        String cacheKey = generateCacheKey(lat, lon, "complete", hours, days);
        
        return weatherCache.get(cacheKey, key -> {
            WeatherProvider provider = selectProvider();
            if (provider == null) {
                throw new RuntimeException("No weather provider available");
            }
            
            try {
                log.info("Fetching complete weather data from {}", provider.getName());
                return provider.getWeatherData(lat, lon, true, hours, days);
            } catch (Exception e) {
                log.error("Failed to fetch complete weather data", e);
                throw new RuntimeException("Failed to fetch weather data", e);
            }
        });
    }
    
    /**
     * 캐시 통계 조회
     */
    public CacheStats getCacheStats() {
        var stats = weatherCache.stats();
        return CacheStats.builder()
            .size(weatherCache.estimatedSize())
            .hitCount(stats.hitCount())
            .missCount(stats.missCount())
            .hitRate(stats.hitRate())
            .evictionCount(stats.evictionCount())
            .build();
    }
    
    /**
     * 캐시 초기화
     */
    public void clearCache() {
        weatherCache.invalidateAll();
        log.info("Weather cache cleared");
    }
    
    /**
     * 제공자 선택 (우선순위 기반)
     */
    private WeatherProvider selectProvider() {
        // 사용 가능한 첫 번째 제공자 반환
        return weatherProviders.stream()
            .filter(WeatherProvider::isAvailable)
            .findFirst()
            .orElse(null);
    }
    
    /**
     * 대체 제공자 선택
     */
    private WeatherProvider selectFallbackProvider(WeatherProvider excludeProvider) {
        return weatherProviders.stream()
            .filter(p -> p.isAvailable() && !p.equals(excludeProvider))
            .findFirst()
            .orElse(null);
    }
    
    /**
     * 캐시 키 생성
     */
    private String generateCacheKey(double lat, double lon, String type, int hours, int days) {
        // 위도/경도를 소수점 2자리로 반올림하여 캐시 효율성 향상
        lat = Math.round(lat * 100.0) / 100.0;
        lon = Math.round(lon * 100.0) / 100.0;
        return String.format("%s:%.2f:%.2f:%s:%d:%d", "weather", lat, lon, type, hours, days);
    }
    
    /**
     * 캐시 통계 DTO
     */
    @lombok.Builder
    @lombok.Data
    public static class CacheStats {
        private long size;
        private long hitCount;
        private long missCount;
        private double hitRate;
        private long evictionCount;
    }
}