package com.dsai.rag.api.weather.service;

import com.dsai.rag.api.weather.model.WeatherData;
import com.dsai.rag.api.weather.model.WeatherSlots;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.output.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * LangChain4j 기반 날씨 에이전트 서비스
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class WeatherAgentService {
    
    private final WeatherService weatherService;
    private final ObjectMapper objectMapper;
    
    @Value("${ollama.base-url:http://localhost:11434}")
    private String ollamaBaseUrl;
    
    @Value("${ollama.chat-model:llama3.2:3b}")
    private String chatModel;
    
    private ChatLanguageModel chatLanguageModel;
    
    private static final String WEATHER_INTENT_PROMPT = """
        당신은 날씨 정보를 분석하는 AI 비서입니다.
        사용자의 한국어 요청에서 날씨 관련 의도와 정보를 추출해주세요.
        
        가능한 의도(intent):
        - CURRENT: 현재 날씨
        - HOURLY: 시간별 예보 
        - DAILY: 일별 예보
        - UNKNOWN: 알 수 없음
        
        추출할 정보:
        - place: 장소/위치 (예: "서울", "부산 해운대", "강남역")
        - date: 날짜 (오늘/내일/모레 또는 특정 날짜)
        - timeOfDay: 시간대 (아침/오전/오후/저녁/밤)
        - hours: 시간별 예보 시간 (기본값: 6)
        - days: 일별 예보 일수 (기본값: 3)
        
        반드시 아래 JSON 형식으로만 응답하세요:
        {
            "intent": "의도",
            "place": "장소",
            "date": "날짜",
            "timeOfDay": "시간대",
            "hours": 숫자,
            "days": 숫자
        }
        
        사용자 메시지: "{{message}}"
        """;
    
    @PostConstruct
    public void init() {
        try {
            log.info("Initializing WeatherAgentService with Ollama model: {}", chatModel);
            chatLanguageModel = OllamaChatModel.builder()
                .baseUrl(ollamaBaseUrl)
                .modelName(chatModel)
                .temperature(0.3)
                .timeout(java.time.Duration.ofSeconds(30))
                .build();
            log.info("Successfully initialized Weather Agent with Ollama");
        } catch (Exception e) {
            log.warn("Failed to initialize Ollama chat model, will use fallback", e);
        }
    }
    
    /**
     * 자연어 메시지 처리
     */
    public Map<String, Object> processMessage(String message) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 1. 의도 분석
            WeatherSlots slots = analyzeIntent(message);
            log.info("Analyzed slots: {}", slots);
            
            // 2. 위치 해결 (지오코딩)
            resolveLocation(slots);
            
            // 3. 날씨 데이터 조회
            WeatherData weatherData = weatherService.getWeatherData(slots);
            
            // 4. 자연어 응답 생성
            String naturalResponse = generateNaturalResponse(weatherData, slots);
            
            response.put("success", true);
            response.put("message", naturalResponse);
            response.put("data", weatherData);
            response.put("slots", slots);
            
        } catch (Exception e) {
            log.error("Error processing weather message", e);
            response.put("success", false);
            response.put("message", "날씨 정보를 조회하는 중 오류가 발생했습니다: " + e.getMessage());
            response.put("error", e.getMessage());
        }
        
        return response;
    }
    
    /**
     * AI를 사용한 의도 분석
     */
    private WeatherSlots analyzeIntent(String message) {
        // AI 분석 시도
        if (chatLanguageModel != null) {
            try {
                String prompt = WEATHER_INTENT_PROMPT.replace("{{message}}", message);
                Response<AiMessage> response = chatLanguageModel.generate(UserMessage.from(prompt));
                String aiResponse = response.content().text();
                
                log.debug("AI intent analysis response: {}", aiResponse);
                
                // JSON 파싱
                String jsonText = extractJsonFromText(aiResponse);
                if (jsonText != null && !jsonText.isEmpty()) {
                    Map<String, Object> analysisResult = objectMapper.readValue(jsonText, Map.class);
                    return mapToWeatherSlots(analysisResult);
                }
            } catch (Exception e) {
                log.warn("AI intent analysis failed, falling back to keyword analysis", e);
            }
        }
        
        // 키워드 기반 분석 (폴백)
        return analyzeIntentByKeyword(message);
    }
    
    /**
     * 키워드 기반 의도 분석 (폴백)
     */
    private WeatherSlots analyzeIntentByKeyword(String message) {
        WeatherSlots slots = new WeatherSlots();
        String lowerMessage = message.toLowerCase();
        
        // 의도 파악
        if (lowerMessage.contains("시간") || lowerMessage.contains("hourly")) {
            slots.setIntent(WeatherSlots.Intent.HOURLY);
        } else if (lowerMessage.contains("내일") || lowerMessage.contains("모레") || 
                   lowerMessage.contains("일별") || lowerMessage.contains("주간")) {
            slots.setIntent(WeatherSlots.Intent.DAILY);
        } else if (lowerMessage.contains("지금") || lowerMessage.contains("현재")) {
            slots.setIntent(WeatherSlots.Intent.CURRENT);
        } else {
            slots.setIntent(WeatherSlots.Intent.CURRENT); // 기본값
        }
        
        // 장소 추출 (주요 도시명)
        String[] cities = {"서울", "부산", "대구", "인천", "광주", "대전", "울산", "세종", 
                          "제주", "수원", "성남", "고양", "용인", "창원", "청주", "전주", "천안"};
        
        for (String city : cities) {
            if (lowerMessage.contains(city)) {
                slots.setPlaceQuery(city);
                break;
            }
        }
        
        // 날짜 추출
        if (lowerMessage.contains("오늘")) {
            slots.setDate(LocalDate.now().toString());
        } else if (lowerMessage.contains("내일")) {
            slots.setDate(LocalDate.now().plusDays(1).toString());
        } else if (lowerMessage.contains("모레")) {
            slots.setDate(LocalDate.now().plusDays(2).toString());
        }
        
        // 시간대 추출
        if (lowerMessage.contains("아침") || lowerMessage.contains("오전")) {
            slots.setTimeOfDay("morning");
        } else if (lowerMessage.contains("오후")) {
            slots.setTimeOfDay("afternoon");
        } else if (lowerMessage.contains("저녁")) {
            slots.setTimeOfDay("evening");
        } else if (lowerMessage.contains("밤")) {
            slots.setTimeOfDay("night");
        }
        
        // 시간/일수 추출
        Pattern hoursPattern = Pattern.compile("(\\d+)시간");
        Matcher hoursMatcher = hoursPattern.matcher(message);
        if (hoursMatcher.find()) {
            slots.setHours(Integer.parseInt(hoursMatcher.group(1)));
        }
        
        Pattern daysPattern = Pattern.compile("(\\d+)일");
        Matcher daysMatcher = daysPattern.matcher(message);
        if (daysMatcher.find()) {
            slots.setDays(Integer.parseInt(daysMatcher.group(1)));
        }
        
        return slots;
    }
    
    /**
     * AI 분석 결과를 WeatherSlots로 변환
     */
    private WeatherSlots mapToWeatherSlots(Map<String, Object> analysisResult) {
        WeatherSlots slots = new WeatherSlots();
        
        // Intent 매핑
        String intentStr = (String) analysisResult.get("intent");
        if (intentStr != null) {
            try {
                slots.setIntent(WeatherSlots.Intent.valueOf(intentStr.toUpperCase()));
            } catch (IllegalArgumentException e) {
                slots.setIntent(WeatherSlots.Intent.CURRENT);
            }
        }
        
        // 나머지 필드 매핑
        slots.setPlaceQuery((String) analysisResult.get("place"));
        slots.setDate((String) analysisResult.get("date"));
        slots.setTimeOfDay((String) analysisResult.get("timeOfDay"));
        
        Object hoursObj = analysisResult.get("hours");
        if (hoursObj instanceof Number) {
            slots.setHours(((Number) hoursObj).intValue());
        }
        
        Object daysObj = analysisResult.get("days");
        if (daysObj instanceof Number) {
            slots.setDays(((Number) daysObj).intValue());
        }
        
        return slots;
    }
    
    /**
     * 위치 해결 (지오코딩)
     */
    private void resolveLocation(WeatherSlots slots) {
        // 이미 좌표가 있으면 스킵
        if (slots.getLat() != null && slots.getLon() != null) {
            return;
        }
        
        // 장소명으로 좌표 찾기 (간단한 매핑)
        String place = slots.getPlaceQuery();
        if (place != null && !place.isEmpty()) {
            Map<String, double[]> cityCoords = getCityCoordinates();
            
            for (Map.Entry<String, double[]> entry : cityCoords.entrySet()) {
                if (place.contains(entry.getKey())) {
                    double[] coords = entry.getValue();
                    slots.setLat(coords[0]);
                    slots.setLon(coords[1]);
                    log.info("Resolved location '{}' to lat={}, lon={}", place, coords[0], coords[1]);
                    return;
                }
            }
        }
        
        // 기본값: 서울
        slots.setLat(37.5665);
        slots.setLon(126.9780);
        if (slots.getPlaceQuery() == null) {
            slots.setPlaceQuery("서울");
        }
        log.info("Using default location (Seoul)");
    }
    
    /**
     * 주요 도시 좌표 매핑
     */
    private Map<String, double[]> getCityCoordinates() {
        Map<String, double[]> coords = new HashMap<>();
        coords.put("서울", new double[]{37.5665, 126.9780});
        coords.put("부산", new double[]{35.1796, 129.0756});
        coords.put("대구", new double[]{35.8714, 128.6014});
        coords.put("인천", new double[]{37.4563, 126.7052});
        coords.put("광주", new double[]{35.1595, 126.8526});
        coords.put("대전", new double[]{36.3504, 127.3845});
        coords.put("울산", new double[]{35.5384, 129.3114});
        coords.put("제주", new double[]{33.4996, 126.5312});
        coords.put("수원", new double[]{37.2636, 127.0286});
        coords.put("강남", new double[]{37.4979, 127.0276});
        coords.put("목동", new double[]{37.5365, 126.8746});
        coords.put("해운대", new double[]{35.1631, 129.1635});
        return coords;
    }
    
    /**
     * 자연어 응답 생성
     */
    private String generateNaturalResponse(WeatherData data, WeatherSlots slots) {
        StringBuilder response = new StringBuilder();
        
        // 위치 정보
        String location = data.getPlace() != null ? data.getPlace() : slots.getPlaceQuery();
        if (location == null) location = "현재 위치";
        
        // 현재 날씨
        if (data.getCurrent() != null) {
            WeatherData.WeatherCurrent current = data.getCurrent();
            response.append(String.format("🌤️ %s의 현재 날씨는 %s입니다.\n", 
                location, current.getDescription() != null ? current.getDescription() : current.getCondition()));
            response.append(String.format("🌡️ 온도: %.1f°C (체감 %.1f°C)\n", 
                current.getTempC(), current.getFeelsLikeC()));
            response.append(String.format("💧 습도: %d%%\n", current.getHumidity()));
            response.append(String.format("💨 바람: %.1fm/s\n", current.getWindMps()));
            
            if (current.getPrecipMm() > 0) {
                response.append(String.format("☔ 강수량: %.1fmm\n", current.getPrecipMm()));
            }
        }
        
        // 시간별 예보
        if (data.getHourly() != null && !data.getHourly().isEmpty()) {
            response.append("\n📊 시간별 예보:\n");
            int count = 0;
            for (WeatherData.WeatherHourly hourly : data.getHourly()) {
                if (count++ >= 3) break; // 처음 3개만
                response.append(String.format("  • %s - %s, %.1f°C",
                    hourly.getTime().format(DateTimeFormatter.ofPattern("HH:mm")),
                    hourly.getCondition(),
                    hourly.getTempC()));
                if (hourly.getPop() > 0) {
                    response.append(String.format(" (강수확률 %d%%)", hourly.getPop()));
                }
                response.append("\n");
            }
        }
        
        // 일별 예보
        if (data.getDaily() != null && !data.getDaily().isEmpty()) {
            response.append("\n📅 일별 예보:\n");
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd");
            for (WeatherData.WeatherDaily daily : data.getDaily()) {
                LocalDate date = LocalDate.parse(daily.getDate());
                String dayName = getDayName(date);
                response.append(String.format("  • %s (%s) - %s, %.0f°C ~ %.0f°C",
                    dayName,
                    date.format(formatter),
                    daily.getCondition(),
                    daily.getTminC(),
                    daily.getTmaxC()));
                if (daily.getPop() > 0) {
                    response.append(String.format(" (강수확률 %d%%)", daily.getPop()));
                }
                response.append("\n");
            }
        }
        
        // 날씨 팁
        if (data.getTips() != null && !data.getTips().isEmpty()) {
            response.append("\n💡 날씨 팁:\n");
            for (String tip : data.getTips()) {
                response.append("  • ").append(tip).append("\n");
            }
        }
        
        return response.toString();
    }
    
    /**
     * 날짜를 요일명으로 변환
     */
    private String getDayName(LocalDate date) {
        LocalDate today = LocalDate.now();
        if (date.equals(today)) return "오늘";
        if (date.equals(today.plusDays(1))) return "내일";
        if (date.equals(today.plusDays(2))) return "모레";
        
        String[] dayNames = {"월", "화", "수", "목", "금", "토", "일"};
        return dayNames[date.getDayOfWeek().getValue() - 1] + "요일";
    }
    
    /**
     * JSON 추출 유틸리티
     */
    private String extractJsonFromText(String text) {
        if (text == null || text.isEmpty()) {
            return null;
        }
        
        // Remove markdown code blocks
        text = text.replaceAll("```json\\s*", "").replaceAll("```\\s*", "");
        
        // Find JSON object
        int start = text.indexOf("{");
        int end = text.lastIndexOf("}");
        
        if (start >= 0 && end > start) {
            return text.substring(start, end + 1);
        }
        
        return null;
    }
}