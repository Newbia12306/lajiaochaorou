package com.example.demo.service;

import com.example.demo.DTO.FrequentDishStats;
import com.example.demo.model.Dish;
import com.example.demo.repository.OrderRecordRepository;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class LlmService {

    @Autowired
    private DishService dishService;

    @Autowired
    private OrderRecordRepository orderRecordRepository;

    @Value("${llm.api.base-url:https://api.deepseek.com/v1}")
    private String apiBaseUrl;

    @Value("${llm.api.key:}")
    private String apiKey;

    @Value("${llm.chat-model:deepseek-chat}")
    private String chatModel;

    @Value("${llm.use-local:true}")
    private boolean useLocal;

    private ChatLanguageModel chatModelInstance;
    private EmbeddingModel embeddingModelInstance;

    @PostConstruct
    public void init() {
        if (useLocal) {
            this.embeddingModelInstance = new AllMiniLmL6V2EmbeddingModel();
            System.out.println("使用本地 Embedding 模型");
        } else {
            this.chatModelInstance = OpenAiChatModel.builder()
                    .baseUrl(apiBaseUrl)
                    .apiKey(apiKey)
                    .modelName(chatModel)
                    .temperature(0.7)
                    .build();
            
            this.embeddingModelInstance = OpenAiEmbeddingModel.builder()
                    .baseUrl(apiBaseUrl)
                    .apiKey(apiKey)
                    .modelName("text-embedding-ada-002")
                    .build();
            
            System.out.println("使用云端 LLM API: " + apiBaseUrl);
        }
    }

    /**
     * 聊天接口（含个性化 + 菜名匹配）
     * 返回 Map: { "response": String, "dishes": List<Dish> }
     */
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

        // 1. 获取当前登录用户
        String userId = getCurrentUserId();

        // 2. 构建 prompt（含用户历史点单记录）
        String fullPrompt = buildPersonalizedPrompt(userMessage, allDishes, userId);

        try {
            String llmResponse = chatModelInstance.generate(fullPrompt);

            // 3. 匹配 LLM 回复中提到的菜品
            List<Dish> matchedDishes = matchDishesInResponse(llmResponse, allDishes);

            result.put("response", llmResponse);
            result.put("dishes", matchedDishes);
        } catch (Exception e) {
            System.err.println("LLM 调用失败: " + e.getMessage());
            result.put("response", "抱歉，暂时无法处理您的请求");
            result.put("dishes", List.of());
        }

        return result;
    }

    /**
     * 从 SecurityContext 获取当前登录用户的手机号，未登录返回 null
     */
    private String getCurrentUserId() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && auth.getName() != null
                    && !"anonymousUser".equals(auth.getName())) {
                return auth.getName();
            }
        } catch (Exception e) {
            System.err.println("获取当前用户失败: " + e.getMessage());
        }
        return null;
    }

    private List<Dish> getDishesForContext() {
        try {
            List<Dish> allDishes = dishService.getAllDishes();
            return allDishes != null ? allDishes : List.of();
        } catch (Exception e) {
            System.err.println("获取菜品信息失败: " + e.getMessage());
            return List.of();
        }
    }

    /**
     * 构建个性化 prompt，包含用户历史点单记录
     */
    private String buildPersonalizedPrompt(String userMessage, List<Dish> dishes, String userId) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是一个餐厅智能助手，请基于以下菜单回答用户问题。\n\n");

        // 用户历史点单
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

        // 菜单
        prompt.append("【餐厅菜单】\n");
        if (dishes.isEmpty()) {
            prompt.append("暂时没有菜品信息\n");
        } else {
            for (Dish dish : dishes) {
                prompt.append("- ").append(dish.getName());
                if (dish.getSpicy() != null) {
                    prompt.append("（辣度：").append(dish.getSpicy().getLabel()).append("）");
                }
                if (dish.getIsSignature() != null && dish.getIsSignature()) {
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

    /**
     * 在 LLM 回复文本中匹配菜单里的菜名，返回命中的 Dish 列表
     */
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
            System.err.println("生成嵌入失败: " + e.getMessage());
            return new float[384];
        }
    }

    public String recommendDishes(String tastePreference, String dietaryRestrictions) {
        if (chatModelInstance == null) {
            return "LLM 服务未配置，请设置 API Key 或启用本地模式";
        }

        List<Dish> dishes = getDishesForContext();

        StringBuilder prompt = new StringBuilder();
        prompt.append("你是一个餐厅智能助手，请基于以下菜单为用户推荐菜品。\n\n");
        prompt.append("【餐厅菜单】\n");

        for (Dish dish : dishes) {
            prompt.append("- ").append(dish.getName());
            if (dish.getSpicy() != null) {
                prompt.append("（辣度：").append(dish.getSpicy()).append("）");
            }
            if (dish.getIsSignature() != null && dish.getIsSignature()) {
                prompt.append(" 【招牌菜】");
            }
            prompt.append("\n");
        }

        prompt.append("\n用户需求：\n");
        prompt.append("口味偏好：").append(tastePreference != null ? tastePreference : "无特殊偏好").append("\n");
        prompt.append("饮食限制：").append(dietaryRestrictions != null ? dietaryRestrictions : "无限制").append("\n\n");
        prompt.append("请从菜单中推荐合适的菜品，并说明推荐理由。");

        try {
            return chatModelInstance.generate(prompt.toString());
        } catch (Exception e) {
            System.err.println("推荐 LLM 调用失败: " + e.getMessage());
            return "抱歉，暂时无法生成推荐";
        }
    }

    /**
     * 个性化推荐：基于用户历史点单记录，由 DeepSeek 生成个性化菜品推荐
     */
    public String personalizedRecommend(String userId) {
        if (chatModelInstance == null) {
            return "LLM 服务未配置，请设置 API Key 或启用本地模式";
        }

        List<Dish> allDishes = getDishesForContext();
        if (allDishes.isEmpty()) {
            return "当前菜单中没有菜品，无法生成推荐";
        }

        // 查询该用户最常点的 10 道菜作为历史偏好
        List<FrequentDishStats> frequentDishes = orderRecordRepository
                .findFrequentDishesByUserId(userId, PageRequest.of(0, 10));

        StringBuilder prompt = new StringBuilder();
        prompt.append("你是一个餐厅智能助手，请基于用户的点餐历史和当前菜单，为用户提供个性化菜品推荐。\n\n");

        // 用户历史偏好
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

        // 全菜单
        prompt.append("【当前完整菜单】\n");
        for (Dish dish : allDishes) {
            prompt.append("- ").append(dish.getName());
            if (dish.getSpicy() != null) {
                prompt.append("（辣度：").append(dish.getSpicy().getLabel()).append("）");
            }
            if (dish.getIsSignature() != null && dish.getIsSignature()) {
                prompt.append(" 【招牌菜】");
            }
            prompt.append("\n");
        }

        prompt.append("\n");
        prompt.append("请完成以下任务：\n");
        prompt.append("1. 分析用户的饮食口味偏好（基于历史点单记录）；\n");
        prompt.append("2. 从当前菜单中推荐 3-5 道用户可能喜欢但还没点过的菜品；\n");
        prompt.append("3. 如果用户有常点的菜，也可以提醒'您的常点菜品'；\n");
        prompt.append("4. 用友好、专业的语气回复，说明推荐理由。");

        try {
            return chatModelInstance.generate(prompt.toString());
        } catch (Exception e) {
            System.err.println("个性化推荐 LLM 调用失败: " + e.getMessage());
            return "抱歉，暂时无法生成个性化推荐";
        }
    }
}
