package com.example.demo.service;

import com.example.demo.DTO.RecommendationResponse;
import com.example.demo.DTO.RecommendationResponse.DishRecommendation;
import com.example.demo.enums.SearchEngineType;
import com.example.demo.model.Dish;
import com.example.demo.repository.DishRepository;
import com.example.demo.service.search.DatabaseSearchService;
import com.example.demo.service.search.ElasticsearchSearchService;
import com.example.demo.service.search.PgVectorSearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class RecommendationService {

    @Autowired
    private DishRepository dishRepository;

    @Autowired
    private DatabaseSearchService databaseSearchService;

    @Autowired(required = false)
    private ElasticsearchSearchService elasticsearchSearchService;

    @Autowired
    private PgVectorSearchService pgVectorSearchService;

    private List<Dish> allDishes;
    private boolean initialized = false;

    @PostConstruct
    public void init() {
        System.out.println("========================================");
        System.out.println("应用启动，初始化推荐服务...");
        try {
            loadData();
            System.out.println("推荐服务初始化完成，共加载 " + (allDishes != null ? allDishes.size() : 0) + " 道菜品");
            
            System.out.println("可用搜索引擎:");
            System.out.println("  - 数据库搜索: 可用");
            if (elasticsearchSearchService != null && elasticsearchSearchService.isAvailable()) {
                System.out.println("  - Elasticsearch搜索: 可用");
            } else {
                System.out.println("  - Elasticsearch搜索: 不可用");
            }
            if (pgVectorSearchService.isAvailable()) {
                System.out.println("  - PG Vector搜索: 可用");
            } else {
                System.out.println("  - PG Vector搜索: 不可用");
            }
        } catch (Exception e) {
            System.err.println("推荐服务初始化失败: " + e.getMessage());
            e.printStackTrace();
        }
        System.out.println("========================================");
    }

    public synchronized void loadData() {
        System.out.println("========================================");
        System.out.println("开始加载菜品数据...");
        allDishes = dishRepository.findByIsDeletedFalse();
        
        if (allDishes == null || allDishes.isEmpty()) {
            System.out.println("警告：数据库中没有菜品数据");
            initialized = true;
            return;
        }
        
        System.out.println("从数据库加载了 " + allDishes.size() + " 道菜品");
        
        System.out.println("前10道菜品名称：");
        for (int i = 0; i < Math.min(10, allDishes.size()); i++) {
            System.out.println("  " + (i+1) + ". " + allDishes.get(i).getName());
        }
        
        initialized = true;
        System.out.println("数据加载完成");
        System.out.println("========================================");
    }

    public RecommendationResponse recommendByTaste(String tasteKeywords, Integer limit, String engineType) {
        if (!initialized || allDishes == null || allDishes.isEmpty()) {
            loadData();
        }

        if (tasteKeywords == null || tasteKeywords.trim().isEmpty()) {
            RecommendationResponse response = new RecommendationResponse();
            response.setSuccess(false);
            response.setMessage("请输入您想吃的口味");
            return response;
        }

        SearchEngineType searchEngineType;
        try {
            searchEngineType = engineType != null ? SearchEngineType.fromCode(engineType) : SearchEngineType.ELASTICSEARCH;
        } catch (IllegalArgumentException e) {
            RecommendationResponse response = new RecommendationResponse();
            response.setSuccess(false);
            response.setMessage("不支持的搜索引擎类型: " + engineType);
            return response;
        }

        List<Dish> results;
        String engineName;

        switch (searchEngineType) {
            case DATABASE:
                results = databaseSearchService.search(tasteKeywords, limit != null && limit > 0 ? limit : 10);
                engineName = databaseSearchService.getEngineName();
                break;
            case ELASTICSEARCH:
                if (elasticsearchSearchService == null || !elasticsearchSearchService.isAvailable()) {
                    RecommendationResponse response = new RecommendationResponse();
                    response.setSuccess(false);
                    response.setMessage("Elasticsearch搜索当前不可用");
                    return response;
                }
                results = elasticsearchSearchService.search(tasteKeywords, limit != null && limit > 0 ? limit : 10);
                engineName = elasticsearchSearchService.getEngineName();
                break;
            case PG_VECTOR:
                if (!pgVectorSearchService.isAvailable()) {
                    RecommendationResponse response = new RecommendationResponse();
                    response.setSuccess(false);
                    response.setMessage("PG Vector搜索当前不可用");
                    return response;
                }
                results = pgVectorSearchService.search(tasteKeywords, limit != null && limit > 0 ? limit : 10);
                engineName = pgVectorSearchService.getEngineName();
                break;
            default:
                RecommendationResponse response = new RecommendationResponse();
                response.setSuccess(false);
                response.setMessage("不支持的搜索引擎类型");
                return response;
        }

        RecommendationResponse response = new RecommendationResponse();
        response.setSuccess(true);
        response.setQuery(tasteKeywords);
        response.setTotal(results.size());

        List<DishRecommendation> recommendations = results.stream()
                .map(dish -> {
                    DishRecommendation recommendation = new DishRecommendation();
                    recommendation.setDish(dish);
                    recommendation.setScore(calculateRelevanceScore(dish, tasteKeywords));
                    recommendation.setMatchReason(generateMatchReason(dish, tasteKeywords));
                    recommendation.setRelevance(formatRelevance(calculateRelevanceScore(dish, tasteKeywords)));
                    return recommendation;
                })
                .collect(Collectors.toList());

        response.setRecommendations(recommendations);
        response.setMessage("使用 " + engineName + " 进行搜索");
        
        return response;
    }

    private double calculateRelevanceScore(Dish dish, String keywords) {
        String dishName = dish.getName().toLowerCase();
        String lowerKeywords = keywords.toLowerCase();
        
        if (dishName.equals(lowerKeywords)) {
            return 10.0;
        }
        
        if (dishName.startsWith(lowerKeywords)) {
            return 8.0;
        }
        
        if (dishName.contains(lowerKeywords)) {
            return 6.0;
        }
        
        long matchCount = 0;
        String[] words = lowerKeywords.split("\\s+");
        for (String word : words) {
            if (dishName.contains(word)) {
                matchCount++;
            }
        }
        
        return matchCount > 0 ? matchCount * 2.0 : 0.5;
    }

    private String generateMatchReason(Dish dish, String keywords) {
        List<String> reasons = new ArrayList<>();
        
        if (dish.getName().toLowerCase().contains(keywords.toLowerCase())) {
            reasons.add("菜名包含关键词");
        }
        
        if (dish.getSpicy() != null && keywords.contains(dish.getSpicy().getLabel())) {
            reasons.add("辣度匹配");
        }
        
        if (dish.getIsSignature() != null && dish.getIsSignature() && 
            (keywords.contains("招牌") || keywords.contains("特色"))) {
            reasons.add("招牌菜品");
        }
        
        return reasons.isEmpty() ? "相关菜品推荐" : String.join("、", reasons);
    }

    private String formatRelevance(double score) {
        if (score > 5.0) {
            return "非常相关";
        } else if (score > 3.0) {
            return "高度相关";
        } else if (score > 1.5) {
            return "中度相关";
        } else if (score > 0.5) {
            return "低度相关";
        } else {
            return "弱相关";
        }
    }
}
