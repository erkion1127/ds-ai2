package com.dsai.rag.api.weather.provider;

import com.dsai.rag.api.weather.model.WeatherData;

/**
 * 날씨 제공자 인터페이스
 */
public interface WeatherProvider {
    
    /**
     * 현재 날씨 조회
     */
    WeatherData.WeatherCurrent getCurrent(double lat, double lon) throws WeatherProviderException;
    
    /**
     * 시간별 예보 조회
     */
    WeatherData.WeatherHourly[] getHourly(double lat, double lon, int hours) throws WeatherProviderException;
    
    /**
     * 일별 예보 조회
     */
    WeatherData.WeatherDaily[] getDaily(double lat, double lon, int days) throws WeatherProviderException;
    
    /**
     * 통합 날씨 데이터 조회
     */
    WeatherData getWeatherData(double lat, double lon, boolean current, int hours, int days) throws WeatherProviderException;
    
    /**
     * 제공자 이름
     */
    String getName();
    
    /**
     * 제공자 사용 가능 여부
     */
    boolean isAvailable();
    
    /**
     * 날씨 제공자 예외
     */
    class WeatherProviderException extends Exception {
        public WeatherProviderException(String message) {
            super(message);
        }
        
        public WeatherProviderException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}