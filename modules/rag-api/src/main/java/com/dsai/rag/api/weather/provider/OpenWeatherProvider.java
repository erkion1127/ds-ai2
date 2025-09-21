package com.dsai.rag.api.weather.provider;

import com.dsai.rag.api.weather.model.WeatherData;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * OpenWeather API 제공자
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class OpenWeatherProvider implements WeatherProvider {
    
    private final ObjectMapper objectMapper;
    private WebClient webClient;
    
    @Value("${weather.openweather.api-key:}")
    private String apiKey;
    
    @Value("${weather.openweather.base-url:https://api.openweathermap.org/data/2.5}")
    private String baseUrl;
    
    @Value("${weather.openweather.timeout:5000}")
    private int timeoutMs;
    
    @PostConstruct
    public void init() {
        this.webClient = WebClient.builder()
            .baseUrl(baseUrl)
            .build();
    }
    
    @Override
    public WeatherData.WeatherCurrent getCurrent(double lat, double lon) throws WeatherProviderException {
        if (!isAvailable()) {
            throw new WeatherProviderException("OpenWeather API key not configured");
        }
        
        try {
            String response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                    .path("/weather")
                    .queryParam("lat", lat)
                    .queryParam("lon", lon)
                    .queryParam("appid", apiKey)
                    .queryParam("units", "metric")
                    .queryParam("lang", "kr")
                    .build())
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofMillis(timeoutMs))
                .block();
            
            return parseCurrent(response);
        } catch (WebClientResponseException e) {
            log.error("OpenWeather API error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new WeatherProviderException("Failed to fetch current weather: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error fetching current weather", e);
            throw new WeatherProviderException("Error fetching current weather", e);
        }
    }
    
    @Override
    public WeatherData.WeatherHourly[] getHourly(double lat, double lon, int hours) throws WeatherProviderException {
        if (!isAvailable()) {
            throw new WeatherProviderException("OpenWeather API key not configured");
        }
        
        try {
            // OpenWeather의 경우 forecast API 사용 (3시간 간격)
            String response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                    .path("/forecast")
                    .queryParam("lat", lat)
                    .queryParam("lon", lon)
                    .queryParam("appid", apiKey)
                    .queryParam("units", "metric")
                    .queryParam("lang", "kr")
                    .queryParam("cnt", Math.min(hours / 3 + 1, 16)) // 최대 5일 (40개 * 3시간)
                    .build())
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofMillis(timeoutMs))
                .block();
            
            return parseHourly(response, hours);
        } catch (Exception e) {
            log.error("Error fetching hourly forecast", e);
            throw new WeatherProviderException("Error fetching hourly forecast", e);
        }
    }
    
    @Override
    public WeatherData.WeatherDaily[] getDaily(double lat, double lon, int days) throws WeatherProviderException {
        if (!isAvailable()) {
            throw new WeatherProviderException("OpenWeather API key not configured");
        }
        
        try {
            // OpenWeather One Call API 3.0 사용 (구독 필요)
            // 대안: forecast API를 일별로 집계
            String response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                    .path("/forecast")
                    .queryParam("lat", lat)
                    .queryParam("lon", lon)
                    .queryParam("appid", apiKey)
                    .queryParam("units", "metric")
                    .queryParam("lang", "kr")
                    .build())
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofMillis(timeoutMs))
                .block();
            
            return parseDaily(response, days);
        } catch (Exception e) {
            log.error("Error fetching daily forecast", e);
            throw new WeatherProviderException("Error fetching daily forecast", e);
        }
    }
    
    @Override
    public WeatherData getWeatherData(double lat, double lon, boolean current, int hours, int days) 
            throws WeatherProviderException {
        WeatherData.WeatherDataBuilder builder = WeatherData.builder()
            .lat(lat)
            .lon(lon)
            .timezone("Asia/Seoul");
        
        if (current) {
            builder.current(getCurrent(lat, lon));
        }
        
        if (hours > 0) {
            WeatherData.WeatherHourly[] hourlyData = getHourly(lat, lon, hours);
            builder.hourly(List.of(hourlyData));
        }
        
        if (days > 0) {
            WeatherData.WeatherDaily[] dailyData = getDaily(lat, lon, days);
            builder.daily(List.of(dailyData));
        }
        
        WeatherData data = builder.build();
        data.setSummary(generateSummary(data));
        data.setTips(generateTips(data));
        
        return data;
    }
    
    @Override
    public String getName() {
        return "OpenWeather";
    }
    
    @Override
    public boolean isAvailable() {
        return apiKey != null && !apiKey.isEmpty();
    }
    
    private WeatherData.WeatherCurrent parseCurrent(String json) throws Exception {
        JsonNode root = objectMapper.readTree(json);
        
        return WeatherData.WeatherCurrent.builder()
            .time(LocalDateTime.now())
            .condition(root.path("weather").get(0).path("main").asText())
            .description(root.path("weather").get(0).path("description").asText())
            .tempC(root.path("main").path("temp").asDouble())
            .feelsLikeC(root.path("main").path("feels_like").asDouble())
            .windMps(root.path("wind").path("speed").asDouble())
            .windDeg(root.path("wind").path("deg").asInt())
            .humidity(root.path("main").path("humidity").asInt())
            .pressure(root.path("main").path("pressure").asInt())
            .clouds(root.path("clouds").path("all").asInt())
            .visibility(root.path("visibility").asDouble(10000) / 1000.0) // m to km
            .precipMm(root.path("rain").path("1h").asDouble(0.0))
            .build();
    }
    
    private WeatherData.WeatherHourly[] parseHourly(String json, int hours) throws Exception {
        JsonNode root = objectMapper.readTree(json);
        JsonNode list = root.path("list");
        
        List<WeatherData.WeatherHourly> hourlyList = new ArrayList<>();
        int count = 0;
        
        for (JsonNode item : list) {
            if (count >= hours / 3) break;
            
            LocalDateTime time = LocalDateTime.ofInstant(
                Instant.ofEpochSecond(item.path("dt").asLong()),
                ZoneId.of("Asia/Seoul")
            );
            
            WeatherData.WeatherHourly hourly = WeatherData.WeatherHourly.builder()
                .time(time)
                .condition(item.path("weather").get(0).path("main").asText())
                .tempC(item.path("main").path("temp").asDouble())
                .feelsLikeC(item.path("main").path("feels_like").asDouble())
                .pop(Math.round(item.path("pop").floatValue() * 100))
                .precipMm(item.path("rain").path("3h").asDouble(0.0))
                .windMps(item.path("wind").path("speed").asDouble())
                .humidity(item.path("main").path("humidity").asInt())
                .clouds(item.path("clouds").path("all").asInt())
                .build();
            
            hourlyList.add(hourly);
            count++;
        }
        
        return hourlyList.toArray(new WeatherData.WeatherHourly[0]);
    }
    
    private WeatherData.WeatherDaily[] parseDaily(String json, int days) throws Exception {
        JsonNode root = objectMapper.readTree(json);
        JsonNode list = root.path("list");
        
        // Forecast API는 3시간 단위이므로 일별로 집계 필요
        // 간단한 구현: 하루당 첫 번째와 마지막 예보만 사용
        List<WeatherData.WeatherDaily> dailyList = new ArrayList<>();
        
        String currentDate = "";
        double minTemp = Double.MAX_VALUE;
        double maxTemp = Double.MIN_VALUE;
        double totalRain = 0;
        int maxPop = 0;
        String condition = "";
        
        for (JsonNode item : list) {
            LocalDateTime time = LocalDateTime.ofInstant(
                Instant.ofEpochSecond(item.path("dt").asLong()),
                ZoneId.of("Asia/Seoul")
            );
            
            String date = time.toLocalDate().toString();
            
            if (!date.equals(currentDate)) {
                if (!currentDate.isEmpty() && dailyList.size() < days) {
                    // 이전 날짜 데이터 저장
                    dailyList.add(WeatherData.WeatherDaily.builder()
                        .date(currentDate)
                        .condition(condition)
                        .tminC(minTemp)
                        .tmaxC(maxTemp)
                        .pop(maxPop)
                        .rainMm(totalRain)
                        .build());
                }
                
                // 새 날짜 시작
                currentDate = date;
                minTemp = Double.MAX_VALUE;
                maxTemp = Double.MIN_VALUE;
                totalRain = 0;
                maxPop = 0;
                condition = item.path("weather").get(0).path("main").asText();
            }
            
            double temp = item.path("main").path("temp").asDouble();
            minTemp = Math.min(minTemp, item.path("main").path("temp_min").asDouble(temp));
            maxTemp = Math.max(maxTemp, item.path("main").path("temp_max").asDouble(temp));
            totalRain += item.path("rain").path("3h").asDouble(0.0);
            maxPop = Math.max(maxPop, Math.round(item.path("pop").floatValue() * 100));
        }
        
        // 마지막 날짜 추가
        if (!currentDate.isEmpty() && dailyList.size() < days) {
            dailyList.add(WeatherData.WeatherDaily.builder()
                .date(currentDate)
                .condition(condition)
                .tminC(minTemp)
                .tmaxC(maxTemp)
                .pop(maxPop)
                .rainMm(totalRain)
                .build());
        }
        
        return dailyList.toArray(new WeatherData.WeatherDaily[0]);
    }
    
    private String generateSummary(WeatherData data) {
        if (data.getCurrent() == null) return "";
        
        WeatherData.WeatherCurrent current = data.getCurrent();
        return String.format("%s 현재 %s, %.1f°C(체감 %.1f°C), 습도 %d%%, 바람 %.1fm/s",
            data.getPlace() != null ? data.getPlace() : "현재 위치",
            current.getCondition(),
            current.getTempC(),
            current.getFeelsLikeC(),
            current.getHumidity(),
            current.getWindMps()
        );
    }
    
    private List<String> generateTips(WeatherData data) {
        List<String> tips = new ArrayList<>();
        
        if (data.getCurrent() != null) {
            WeatherData.WeatherCurrent current = data.getCurrent();
            
            // 온도 기반 팁
            if (current.getTempC() < 0) {
                tips.add("매우 추워요. 따뜻한 옷을 입으세요");
            } else if (current.getTempC() > 30) {
                tips.add("매우 더워요. 시원한 옷을 입고 수분 보충하세요");
            }
            
            // 강수 팁
            if (current.getPrecipMm() > 0) {
                tips.add("비가 오고 있어요. 우산을 챙기세요");
            }
            
            // UV 팁
            if (current.getUvIndex() != null && current.getUvIndex() > 6) {
                tips.add("자외선 지수 높음: 선크림을 바르세요");
            }
            
            // 풍속 팁
            if (current.getWindMps() > 10) {
                tips.add("바람이 강해요. 주의하세요");
            }
        }
        
        return tips;
    }
}