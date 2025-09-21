package com.dsai.rag.api.weather.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 날씨 데이터 모델
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeatherData {
    
    private String place;           // 위치 표시명
    private Double lat;             // 위도
    private Double lon;             // 경도
    private String timezone;        // 시간대
    private WeatherCurrent current; // 현재 날씨
    private List<WeatherHourly> hourly; // 시간별 예보
    private List<WeatherDaily> daily;   // 일별 예보
    private String summary;         // 요약 설명
    private List<String> tips;      // 날씨 팁
    
    /**
     * 현재 날씨 정보
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WeatherCurrent {
        private LocalDateTime time;      // 시간
        private String condition;         // 날씨 상태
        private String description;       // 상세 설명
        private Double tempC;            // 온도 (°C)
        private Double feelsLikeC;       // 체감 온도 (°C)
        private Double windMps;          // 풍속 (m/s)
        private Integer windDeg;         // 풍향 (degrees)
        private Integer humidity;        // 습도 (%)
        private Double precipMm;         // 강수량 (mm)
        private Integer pressure;        // 기압 (hPa)
        private Integer clouds;          // 구름 (%)
        private Double visibility;       // 가시거리 (km)
        private Double uvIndex;          // UV 지수
    }
    
    /**
     * 시간별 예보 정보
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WeatherHourly {
        private LocalDateTime time;      // 시간
        private String condition;        // 날씨 상태
        private Double tempC;           // 온도 (°C)
        private Double feelsLikeC;      // 체감 온도 (°C)
        private Integer pop;            // 강수 확률 (%)
        private Double precipMm;        // 강수량 (mm)
        private Double windMps;         // 풍속 (m/s)
        private Integer humidity;       // 습도 (%)
        private Integer clouds;         // 구름 (%)
    }
    
    /**
     * 일별 예보 정보
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WeatherDaily {
        private String date;            // 날짜 (yyyy-MM-dd)
        private String condition;       // 날씨 상태
        private String description;     // 상세 설명
        private Double tminC;          // 최저 온도 (°C)
        private Double tmaxC;          // 최고 온도 (°C)
        private Integer pop;           // 강수 확률 (%)
        private Double rainMm;         // 강수량 (mm)
        private Double windMps;        // 풍속 (m/s)
        private Integer humidity;      // 습도 (%)
        private Double uvIndex;        // UV 지수
        private LocalDateTime sunrise; // 일출 시간
        private LocalDateTime sunset;  // 일몰 시간
    }
}