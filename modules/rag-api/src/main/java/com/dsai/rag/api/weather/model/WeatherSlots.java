package com.dsai.rag.api.weather.model;

import lombok.Data;

/**
 * 날씨 요청 슬롯 정보
 */
@Data
public class WeatherSlots {
    
    public enum Intent { 
        CURRENT,    // 현재 날씨
        HOURLY,     // 시간별 예보
        DAILY,      // 일별 예보
        ALERT,      // 날씨 경보
        COMPARE,    // 비교
        UNKNOWN     // 알 수 없음
    }
    
    private Intent intent = Intent.UNKNOWN;
    private String placeQuery;      // "서울 목동"
    private Double lat;              // nullable
    private Double lon;              // nullable
    private String date;             // ISO yyyy-MM-dd (옵션)
    private String timeOfDay;        // morning/afternoon/evening/night
    private String unit = "metric";  // metric/imperial
    private String detail = "normal"; // brief/normal/verbose
    private String locale = "ko-KR";
    private String tz = "Asia/Seoul";
    private Integer hours = 6;       // 시간별 예보 시간
    private Integer days = 3;        // 일별 예보 일수
}