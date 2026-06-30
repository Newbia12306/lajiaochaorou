package com.example.demo.controller;

import com.example.demo.DTO.RecommendationResponse;
import com.example.demo.service.RecommendationService;
import com.example.demo.service.VectorSyncService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/recommendations")
public class RecommendationController {

    private static final Logger log = LoggerFactory.getLogger(RecommendationController.class);
    private final RecommendationService recommendationService;
    private final VectorSyncService vectorSyncService;

    public RecommendationController(RecommendationService recommendationService,
                                     VectorSyncService vectorSyncService) {
        this.recommendationService = recommendationService;
        this.vectorSyncService = vectorSyncService;
    }

    @PostMapping("/search")
    public ResponseEntity<RecommendationResponse> searchByTaste(
            @RequestBody Map<String, Object> request) {
        String tasteKeywords = (String) request.get("tasteKeywords");
        Integer limit = request.get("limit") != null ?
                ((Number) request.get("limit")).intValue() : 10;
        String engine = (String) request.getOrDefault("engine", "elasticsearch");

        RecommendationResponse response = recommendationService.recommendByTaste(tasteKeywords, limit, engine);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/search/simple")
    public ResponseEntity<RecommendationResponse> searchByTasteSimple(
            @RequestParam String keywords,
            @RequestParam(defaultValue = "10") Integer limit,
            @RequestParam(defaultValue = "elasticsearch") String engine) {
        RecommendationResponse response = recommendationService.recommendByTaste(keywords, limit, engine);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/engines")
    public ResponseEntity<Map<String, Object>> getAvailableEngines() {
        Map<String, Object> result = new HashMap<>();
        result.put("database", Map.of("code", "database", "name", "数据库搜索", "available", true));
        result.put("elasticsearch", Map.of("code", "elasticsearch", "name", "Elasticsearch搜索"));
        result.put("pg_vector", Map.of("code", "pg_vector", "name", "PG Vector搜索"));
        return ResponseEntity.ok(result);
    }

    @PostMapping("/rebuild-index")
    public ResponseEntity<Map<String, Object>> rebuildIndex() {
        recommendationService.loadData();
        return ResponseEntity.ok(Map.of("success", true, "message", "索引重建成功"));
    }

    @PostMapping("/sync-vectors")
    public ResponseEntity<Map<String, Object>> syncVectors() {
        try {
            vectorSyncService.syncAllVectors();
            return ResponseEntity.ok(Map.of("success", true, "message", "向量同步成功"));
        } catch (Exception e) {
            log.error("Vector sync failed", e);
            return ResponseEntity.status(500).body(
                    Map.of("success", false, "message", "向量同步失败: " + e.getMessage()));
        }
    }
}
