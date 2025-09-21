package com.dsai.rag.api.agent;

import com.dsai.rag.api.entity.Whisky;
import com.dsai.rag.api.repository.WhiskyRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
public class WhiskyAgent {

    private final WhiskyRepository whiskyRepository;
    private final ChatLanguageModel chatModel;
    private final ObjectMapper objectMapper;

    public WhiskyAgent(
            WhiskyRepository whiskyRepository,
            @Value("${ollama.base-url:http://localhost:11434}") String ollamaBaseUrl,
            @Value("${ollama.chat-model:llama3.2:latest}") String modelName) {

        this.whiskyRepository = whiskyRepository;
        this.objectMapper = new ObjectMapper();

        this.chatModel = OllamaChatModel.builder()
                .baseUrl(ollamaBaseUrl)
                .modelName(modelName)
                .temperature(0.7)
                .timeout(java.time.Duration.ofSeconds(300))
                .build();

        log.info("WhiskyAgent initialized with model: {}", modelName);
    }

    @Tool("위스키를 타입별로 추천합니다 (Single Malt, Blended, Bourbon 등)")
    public String recommendByType(String type) {
        try {
            List<Whisky> whiskies = whiskyRepository.findByTypeOrderByRatingDesc(type);
            if (whiskies.isEmpty()) {
                return "해당 타입의 위스키를 찾을 수 없습니다.";
            }
            return formatWhiskyRecommendations(whiskies.subList(0, Math.min(3, whiskies.size())));
        } catch (Exception e) {
            log.error("Error recommending by type", e);
            return "위스키 추천 중 오류가 발생했습니다.";
        }
    }

    @Tool("위스키를 지역별로 추천합니다 (Scotland, Ireland, USA, Japan 등)")
    public String recommendByRegion(String region) {
        try {
            List<Whisky> whiskies = whiskyRepository.findByRegionOrderByRatingDesc(region);
            if (whiskies.isEmpty()) {
                return "해당 지역의 위스키를 찾을 수 없습니다.";
            }
            return formatWhiskyRecommendations(whiskies.subList(0, Math.min(3, whiskies.size())));
        } catch (Exception e) {
            log.error("Error recommending by region", e);
            return "위스키 추천 중 오류가 발생했습니다.";
        }
    }

    @Tool("위스키를 맛 프로필별로 추천합니다 (peaty, smoky, sweet, fruity, spicy 등)")
    public String recommendByFlavor(String flavorProfile) {
        try {
            List<Whisky> whiskies = whiskyRepository.findByFlavorProfileContainingIgnoreCase(flavorProfile);
            if (whiskies.isEmpty()) {
                return "해당 맛 프로필의 위스키를 찾을 수 없습니다.";
            }
            return formatWhiskyRecommendations(whiskies.subList(0, Math.min(3, whiskies.size())));
        } catch (Exception e) {
            log.error("Error recommending by flavor", e);
            return "위스키 추천 중 오류가 발생했습니다.";
        }
    }

    @Tool("가격대별로 위스키를 추천합니다 (1-5, 1:저가, 5:프리미엄)")
    public String recommendByPriceRange(int priceRange) {
        try {
            List<Whisky> whiskies = whiskyRepository.findByPriceRangeLessThanEqualOrderByRatingDesc(priceRange);
            if (whiskies.isEmpty()) {
                return "해당 가격대의 위스키를 찾을 수 없습니다.";
            }
            return formatWhiskyRecommendations(whiskies.subList(0, Math.min(3, whiskies.size())));
        } catch (Exception e) {
            log.error("Error recommending by price", e);
            return "위스키 추천 중 오류가 발생했습니다.";
        }
    }

    @Tool("상황별 위스키를 추천합니다 (입문용, 선물용, 특별한날 등)")
    public String recommendByOccasion(String occasion) {
        try {
            List<Whisky> whiskies = whiskyRepository.findByOccasionContainingIgnoreCase(occasion);
            if (whiskies.isEmpty()) {
                return "해당 상황에 맞는 위스키를 찾을 수 없습니다.";
            }
            return formatWhiskyRecommendations(whiskies.subList(0, Math.min(3, whiskies.size())));
        } catch (Exception e) {
            log.error("Error recommending by occasion", e);
            return "위스키 추천 중 오류가 발생했습니다.";
        }
    }

    @Tool("위스키에 대한 상세 정보를 제공합니다")
    public String getWhiskyDetails(String whiskyName) {
        try {
            List<Whisky> whiskies = whiskyRepository.searchWhiskies(whiskyName);
            if (whiskies.isEmpty()) {
                return "해당 위스키를 찾을 수 없습니다.";
            }

            Whisky whisky = whiskies.get(0);
            return formatWhiskyDetails(whisky);
        } catch (Exception e) {
            log.error("Error getting whisky details", e);
            return "위스키 정보 조회 중 오류가 발생했습니다.";
        }
    }

    @Tool("인기 위스키 TOP 10을 추천합니다")
    public String getTopRatedWhiskies() {
        try {
            List<Whisky> whiskies = whiskyRepository.findTop10ByOrderByRatingDesc();
            if (whiskies.isEmpty()) {
                return "위스키 데이터가 없습니다.";
            }

            StringBuilder sb = new StringBuilder("🥃 **인기 위스키 TOP 10**\n\n");
            for (int i = 0; i < whiskies.size(); i++) {
                Whisky w = whiskies.get(i);
                sb.append(String.format("%d. **%s** (%s)\n", i + 1, w.getName(), w.getBrand()));
                sb.append(String.format("   - 타입: %s | 지역: %s\n", w.getType(), w.getRegion()));
                sb.append(String.format("   - 평점: %.1f/5.0\n", w.getRating()));
                if (w.getAge() != null) {
                    sb.append(String.format("   - 숙성: %d년\n", w.getAge()));
                }
                sb.append("\n");
            }

            return sb.toString();
        } catch (Exception e) {
            log.error("Error getting top rated whiskies", e);
            return "인기 위스키 조회 중 오류가 발생했습니다.";
        }
    }

    public Map<String, Object> processWhiskyQuery(String query) {
        Map<String, Object> response = new HashMap<>();

        try {
            // AI를 사용한 자연어 처리
            String aiResponse = analyzeWhiskyQuery(query);
            response.put("response", aiResponse);
            response.put("success", true);

            // 추가 추천 위스키
            List<Whisky> recommendations = getRecommendations(query);
            if (!recommendations.isEmpty()) {
                response.put("recommendations", recommendations);
            }

        } catch (Exception e) {
            log.error("Error processing whisky query", e);
            response.put("response", "위스키 추천 처리 중 오류가 발생했습니다.");
            response.put("success", false);
        }

        return response;
    }

    private String analyzeWhiskyQuery(String query) {
        String prompt = String.format("""
            당신은 전문 위스키 소믈리에입니다. 다음 질문에 대해 친절하고 전문적으로 답변해주세요.

            질문: %s

            답변 시 다음 사항을 포함해주세요:
            1. 위스키의 특징과 역사
            2. 테이스팅 노트
            3. 추천 음식 페어링
            4. 적절한 음용 방법

            답변:
            """, query);

        return chatModel.generate(prompt);
    }

    private List<Whisky> getRecommendations(String query) {
        String lowerQuery = query.toLowerCase();

        // 키워드 기반 추천
        if (lowerQuery.contains("입문") || lowerQuery.contains("처음") || lowerQuery.contains("초보")) {
            return whiskyRepository.findByOccasionContainingIgnoreCase("입문용");
        } else if (lowerQuery.contains("선물") || lowerQuery.contains("gift")) {
            return whiskyRepository.findByOccasionContainingIgnoreCase("선물용");
        } else if (lowerQuery.contains("피트") || lowerQuery.contains("스모키")) {
            return whiskyRepository.findByFlavorProfileContainingIgnoreCase("peaty");
        } else if (lowerQuery.contains("달콤") || lowerQuery.contains("스위트")) {
            return whiskyRepository.findByFlavorProfileContainingIgnoreCase("sweet");
        }

        // 기본 추천
        return whiskyRepository.findTop10ByOrderByRatingDesc()
                .stream()
                .limit(3)
                .collect(Collectors.toList());
    }

    private String formatWhiskyRecommendations(List<Whisky> whiskies) {
        StringBuilder sb = new StringBuilder("🥃 **추천 위스키**\n\n");

        for (Whisky w : whiskies) {
            sb.append(String.format("**%s** (%s)\n", w.getName(), w.getBrand()));
            sb.append(String.format("- 타입: %s | 지역: %s\n", w.getType(), w.getRegion()));
            if (w.getAge() != null) {
                sb.append(String.format("- 숙성: %d년 | ABV: %.1f%%\n", w.getAge(), w.getAbv()));
            }
            sb.append(String.format("- 맛: %s\n", w.getFlavorProfile()));
            if (w.getTastingNotes() != null) {
                sb.append(String.format("- 테이스팅: %s\n", w.getTastingNotes()));
            }
            sb.append(String.format("- 평점: %.1f/5.0\n", w.getRating()));
            sb.append("\n");
        }

        return sb.toString();
    }

    private String formatWhiskyDetails(Whisky whisky) {
        StringBuilder sb = new StringBuilder();

        sb.append(String.format("🥃 **%s**\n", whisky.getName()));
        sb.append(String.format("브랜드: %s\n\n", whisky.getBrand()));

        sb.append("**기본 정보**\n");
        sb.append(String.format("- 타입: %s\n", whisky.getType()));
        sb.append(String.format("- 지역: %s\n", whisky.getRegion()));
        if (whisky.getAge() != null) {
            sb.append(String.format("- 숙성 년수: %d년\n", whisky.getAge()));
        }
        if (whisky.getAbv() != null) {
            sb.append(String.format("- 알코올 도수: %.1f%%\n", whisky.getAbv()));
        }
        sb.append("\n");

        if (whisky.getDescription() != null) {
            sb.append("**설명**\n");
            sb.append(whisky.getDescription()).append("\n\n");
        }

        if (whisky.getTastingNotes() != null) {
            sb.append("**테이스팅 노트**\n");
            sb.append(whisky.getTastingNotes()).append("\n\n");
        }

        if (whisky.getFlavorProfile() != null) {
            sb.append("**맛 프로필**\n");
            sb.append(whisky.getFlavorProfile()).append("\n\n");
        }

        if (whisky.getFoodPairing() != null) {
            sb.append("**추천 페어링**\n");
            sb.append(whisky.getFoodPairing()).append("\n\n");
        }

        if (whisky.getOccasion() != null) {
            sb.append("**추천 상황**\n");
            sb.append(whisky.getOccasion()).append("\n\n");
        }

        sb.append(String.format("**평점**: %.1f/5.0\n", whisky.getRating()));

        return sb.toString();
    }
}