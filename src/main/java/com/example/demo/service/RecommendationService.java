package com.example.demo.service;

import com.example.demo.DTO.RecommendationResponse;
import com.example.demo.DTO.RecommendationResponse.DishRecommendation;
import com.example.demo.algorithm.BM25Algorithm;
import com.example.demo.algorithm.BM25Algorithm.SearchResult;
import com.example.demo.algorithm.TextPreprocessor;
import com.example.demo.model.Dish;
import com.example.demo.repository.DishRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class RecommendationService {

    @Autowired
    private DishRepository dishRepository;

    private BM25Algorithm bm25Algorithm;
    private TextPreprocessor textPreprocessor;
    private List<Dish> allDishes;
    private boolean indexBuilt = false;

    public RecommendationService() {
        this.bm25Algorithm = new BM25Algorithm();
        this.textPreprocessor = new TextPreprocessor();
    }

    @PostConstruct
    public void init() {
        System.out.println("========================================");
        System.out.println("应用启动，开始构建 BM25 索引...");
        try {
            rebuildIndex();
            System.out.println("BM25 索引构建完成，共索引 " + (allDishes != null ? allDishes.size() : 0) + " 道菜品");
        } catch (Exception e) {
            System.err.println("索引构建失败: " + e.getMessage());
            e.printStackTrace();
        }
        System.out.println("========================================");
    }

    public synchronized void rebuildIndex() {
        System.out.println("========================================");
        System.out.println("开始重建索引...");
        allDishes = dishRepository.findByIsDeletedFalse();
        
        if (allDishes == null || allDishes.isEmpty()) {
            System.out.println("警告：数据库中没有菜品数据，索引为空");
            indexBuilt = true;
            return;
        }
        
        System.out.println("从数据库加载了 " + allDishes.size() + " 道菜品");
        
        System.out.println("前10道菜品名称：");
        for (int i = 0; i < Math.min(10, allDishes.size()); i++) {
            System.out.println("  " + (i+1) + ". " + allDishes.get(i).getName());
        }
        
        List<String> documents = allDishes.stream()
                .map(dish -> textPreprocessor.preprocessDish(
                        dish.getName(),
                        dish.getSpicy() != null ? dish.getSpicy().getLabel() : "",
                        dish.getIsSignature() != null && dish.getIsSignature()))
                .collect(Collectors.toList());
        
        System.out.println("前5个文档内容（预处理后）：");
        for (int i = 0; i < Math.min(5, documents.size()); i++) {
            System.out.println("  " + (i+1) + ". " + documents.get(i));
        }
        
        bm25Algorithm.buildIndex(documents);
        indexBuilt = true;
        System.out.println("索引构建完成");
        System.out.println("========================================");
    }

    public RecommendationResponse recommendByTaste(String tasteKeywords, Integer limit) {
        if (!indexBuilt || allDishes == null || allDishes.isEmpty()) {
            rebuildIndex();
        }

        if (tasteKeywords == null || tasteKeywords.trim().isEmpty()) {
            RecommendationResponse response = new RecommendationResponse();
            response.setSuccess(false);
            response.setMessage("请输入您想吃的口味");
            return response;
        }

        int maxResults = (limit != null && limit > 0) ? limit : 10;
        List<SearchResult> results = bm25Algorithm.search(tasteKeywords, maxResults);

        RecommendationResponse response = new RecommendationResponse();
        response.setSuccess(true);
        response.setQuery(tasteKeywords);
        response.setTotal(results.size());

        List<DishRecommendation> recommendations = results.stream()
                .map(result -> {
                    if (result.getDocumentId() >= 0 && result.getDocumentId() < allDishes.size()) {
                        Dish dish = allDishes.get(result.getDocumentId());
                        DishRecommendation recommendation = new DishRecommendation();
                        recommendation.setDish(dish);
                        recommendation.setScore(result.getScore());
                        recommendation.setMatchReason(generateMatchReason(dish, tasteKeywords));
                        recommendation.setRelevance(formatRelevance(result.getScore()));
                        return recommendation;
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        response.setRecommendations(recommendations);
        return response;
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
