package com.example.demo.service;

import com.example.demo.dto.FrequentDishStats;
import com.example.demo.model.Dish;
import com.example.demo.repository.OrderRecordRepository;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.*;

@Service
public class LlmService {

    private static final Logger log = LoggerFactory.getLogger(LlmService.class);

    private final DishService dishService;
    private final OrderRecordRepository orderRecordRepository;
    private final String apiBaseUrl;
    private final String apiKey;
    private final String chatModelName;
    private final boolean useLocal;

    private ChatLanguageModel chatModelInstance;
    private EmbeddingModel embeddingModelInstance;

    public LlmService(DishService dishService,
                      OrderRecordRepository orderRecordRepository,
                      @Value("${llm.api.base-url:https://api.deepseek.com/v1}") String apiBaseUrl,
                      @Value("${llm.api.key:}") String apiKey,
                      @Value("${llm.chat-model:deepseek-chat}") String chatModelName,
                      @Value("${llm.use-local:true}") boolean useLocal) {
        this.dishService = dishService;
        this.orderRecordRepository = orderRecordRepository;
        this.apiBaseUrl = apiBaseUrl;
        this.apiKey = apiKey;
        this.chatModelName = chatModelName;
        this.useLocal = useLocal;
    }

    @PostConstruct
    public void init() {
        if (useLocal) {
            this.embeddingModelInstance = new AllMiniLmL6V2EmbeddingModel();
            log.info("使用本地 Embedding 模型");
        } else {
            this.chatModelInstance = OpenAiChatModel.builder()
                    .baseUrl(apiBaseUrl)
                    .apiKey(apiKey)
                    .modelName(chatModelName)
                    .temperature(0.7)
                    .build();

            this.embeddingModelInstance = OpenAiEmbeddingModel.builder()
                    .baseUrl(apiBaseUrl)
                    .apiKey(apiKey)
                    .modelName("text-embedding-ada-002")
                    .build();

            log.info("使用云端 LLM API: {}", apiBaseUrl);
        }
    }

    public Map<String, Object> chat(String userMessage) {
        Map<String, Object> result = new HashMap<>();

        if (chatModelInstance == null) {
            result.put("response", "LLM 服务未配置，请设置 API Key 或启用本地模式");
            result.put("dishes", List.of());
            return result;
        }

        List<Dish> allDishes = getDishesForContext();
        if (allDishes.isEmpty()) {
            result.put("response", "当前菜单中没有菜品信息，无法为您提供建议。");
            result.put("dishes", List.of());
            return result;
        }

        String userId = getCurrentUserId();
        String fullPrompt = buildPersonalizedPrompt(userMessage, allDishes, userId);

        try {
            String llmResponse = chatModelInstance.generate(fullPrompt);
            List<Dish> matchedDishes = matchDishesInResponse(llmResponse, allDishes);
            result.put("response", llmResponse);
            result.put("dishes", matchedDishes);
        } catch (Exception e) {
            log.error("LLM 调用失败", e);
            result.put("response", "抱歉，暂时无法处理您的请求");
            result.put("dishes", List.of());
        }

        return result;
    }

    private String getCurrentUserId() {
        try {
            var auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && auth.getName() != null
                    && !"anonymousUser".equals(auth.getName())) {
                return auth.getName();
            }
        } catch (Exception e) {
            log.warn("获取当前用户失败: {}", e.getMessage());
        }
        return null;
    }

    private List<Dish> getDishesForContext() {
        try {
            List<Dish> allDishes = dishService.getAllDishes();
            return allDishes != null ? allDishes : List.of();
        } catch (Exception e) {
            log.error("获取菜品信息失败", e);
            return List.of();
        }
    }

    private String buildPersonalizedPrompt(String userMessage, List<Dish> dishes, String userId) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是一个餐厅智能助手，请基于以下菜单回答用户问题。\n\n");

        if (userId != null) {
            List<FrequentDishStats> frequentDishes = orderRecordRepository
                    .findFrequentDishesByUserId(userId, PageRequest.of(0, 10));
            if (!frequentDishes.isEmpty()) {
                prompt.append("【该用户的历史点单偏好】\n");
                for (int i = 0; i < frequentDishes.size(); i++) {
                    FrequentDishStats stat = frequentDishes.get(i);
                    prompt.append(i + 1).append(". ").append(stat.getDishName())
                            .append("（累计 ").append(stat.getCount()).append(" 次）\n");
                }
                prompt.append("\n");
            }
        }

        prompt.append("【餐厅菜单】\n");
        if (dishes.isEmpty()) {
            prompt.append("暂时没有菜品信息\n");
        } else {
            for (Dish dish : dishes) {
                prompt.append("- ").append(dish.getName());
                if (dish.getSpicy() != null) {
                    prompt.append("（辣度：").append(dish.getSpicy().getLabel()).append("）");
                }
                if (Boolean.TRUE.equals(dish.getIsSignature())) {
                    prompt.append(" 【招牌菜】");
                }
                prompt.append("\n");
            }
        }

        prompt.append("\n【用户问题】\n");
        prompt.append(userMessage);
        prompt.append("\n\n");
        prompt.append("请用友好、专业的语气回答。如果用户询问推荐，请结合其历史偏好进行个性化推荐。");

        return prompt.toString();
    }

    private List<Dish> matchDishesInResponse(String response, List<Dish> menu) {
        if (response == null || response.isBlank() || menu.isEmpty()) {
            return List.of();
        }

        List<Dish> matched = new ArrayList<>();
        for (Dish dish : menu) {
            if (response.contains(dish.getName())) {
                matched.add(dish);
            }
        }
        return matched;
    }

    public float[] generateEmbedding(String text) {
        if (embeddingModelInstance == null) {
            log.warn("Embedding model not initialized, returning zero vector");
            return new float[384];
        }

        try {
            dev.langchain4j.data.embedding.Embedding embedding =
                    embeddingModelInstance.embed(text).content();
            List<Float> vectorList = embedding.vectorAsList();
            float[] result = new float[vectorList.size()];
            for (int i = 0; i < vectorList.size(); i++) {
                result[i] = vectorList.get(i);
            }
            return result;
        } catch (Exception e) {
            log.error("生成嵌入失败: {}", e.getMessage(), e);
            return new float[384];
        }
    }

    public String recommendDishes(String tastePreference, String dietaryRestrictions) {
        if (chatModelInstance == null) {
            return "LLM 服务未配置，请设置 API Key 或启用本地模式";
        }

        List<Dish> dishes = getDishesForContext();
        String prompt = buildRecommendationPrompt(dishes, tastePreference, dietaryRestrictions);

        try {
            return chatModelInstance.generate(prompt);
        } catch (Exception e) {
            log.error("推荐 LLM 调用失败", e);
            return "抱歉，暂时无法生成推荐";
        }
    }

    public String personalizedRecommend(String userId) {
        if (chatModelInstance == null) {
            return "LLM 服务未配置，请设置 API Key 或启用本地模式";
        }

        List<Dish> allDishes = getDishesForContext();
        if (allDishes.isEmpty()) {
            return "当前菜单中没有菜品，无法生成推荐";
        }

        String prompt = buildPersonalizedRecommendPrompt(userId, allDishes);

        try {
            return chatModelInstance.generate(prompt);
        } catch (Exception e) {
            log.error("个性化推荐 LLM 调用失败", e);
            return "抱歉，暂时无法生成个性化推荐";
        }
    }

    private String buildRecommendationPrompt(List<Dish> dishes, String tastePreference,
                                              String dietaryRestrictions) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是一个餐厅智能助手，请基于以下菜单为用户推荐菜品。\n\n");
        prompt.append("【餐厅菜单】\n");
        appendMenuDishes(prompt, dishes);
        prompt.append("\n用户需求：\n");
        prompt.append("口味偏好：").append(tastePreference != null ? tastePreference : "无特殊偏好").append("\n");
        prompt.append("饮食限制：").append(dietaryRestrictions != null ? dietaryRestrictions : "无限制").append("\n\n");
        prompt.append("请从菜单中推荐合适的菜品，并说明推荐理由。");
        return prompt.toString();
    }

    private String buildPersonalizedRecommendPrompt(String userId, List<Dish> allDishes) {
        List<FrequentDishStats> frequentDishes = orderRecordRepository
                .findFrequentDishesByUserId(userId, PageRequest.of(0, 10));

        StringBuilder prompt = new StringBuilder();
        prompt.append("你是一个餐厅智能助手，请基于用户的点餐历史和当前菜单，为用户提供个性化菜品推荐。\n\n");

        if (frequentDishes.isEmpty()) {
            prompt.append("【用户历史点单】\n该用户暂无点单记录，请根据菜单直接推荐热门菜品。\n\n");
        } else {
            prompt.append("【用户历史点单（按频率排序）】\n");
            for (int i = 0; i < frequentDishes.size(); i++) {
                FrequentDishStats stat = frequentDishes.get(i);
                prompt.append(i + 1).append(". ").append(stat.getDishName())
                        .append(" — 累计点了 ").append(stat.getCount()).append(" 次");
                if (stat.getLastOrderedAt() != null) {
                    prompt.append("（最近一次：").append(stat.getLastOrderedAt().toLocalDate()).append("）");
                }
                prompt.append("\n");
            }
            prompt.append("\n");
        }

        prompt.append("【当前完整菜单】\n");
        appendMenuDishes(prompt, allDishes);
        prompt.append("\n请完成以下任务：\n");
        prompt.append("1. 分析用户的饮食口味偏好（基于历史点单记录）；\n");
        prompt.append("2. 从当前菜单中推荐 3-5 道用户可能喜欢但还没点过的菜品；\n");
        prompt.append("3. 如果用户有常点的菜，也可以提醒'您的常点菜品'；\n");
        prompt.append("4. 用友好、专业的语气回复，说明推荐理由。");
        return prompt.toString();
    }

    private void appendMenuDishes(StringBuilder sb, List<Dish> dishes) {
        for (Dish dish : dishes) {
            sb.append("- ").append(dish.getName());
            if (dish.getSpicy() != null) {
                sb.append("（辣度：").append(dish.getSpicy().getLabel()).append("）");
            }
            if (Boolean.TRUE.equals(dish.getIsSignature())) {
                sb.append(" 【招牌菜】");
            }
            sb.append("\n");
        }
    }
}
