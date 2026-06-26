package com.example.demo.controller;

import com.example.demo.DTO.RecommendationResponse;
import com.example.demo.service.RecommendationService;
import com.example.demo.service.VectorSyncService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/recommendations")
@CrossOrigin(origins = "*")
public class RecommendationController {

    @Autowired
    private RecommendationService recommendationService;

    @Autowired
    private VectorSyncService vectorSyncService;

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
        Map<String, Object> database = new HashMap<>();
        database.put("code", "database");
        database.put("name", "数据库搜索");
        database.put("available", true);
        
        Map<String, Object> elasticsearch = new HashMap<>();
        elasticsearch.put("code", "elasticsearch");
        elasticsearch.put("name", "Elasticsearch搜索");
        
        Map<String, Object> pgVector = new HashMap<>();
        pgVector.put("code", "pg_vector");
        pgVector.put("name", "PG Vector搜索");
        
        result.put("database", database);
        result.put("elasticsearch", elasticsearch);
        result.put("pg_vector", pgVector);
        
        return ResponseEntity.ok(result);
    }

    @PostMapping("/rebuild-index")
    public ResponseEntity<Map<String, Object>> rebuildIndex() {
        recommendationService.loadData();
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "索引重建成功");
        return ResponseEntity.ok(result);
    }

    @PostMapping("/sync-vectors")
    public ResponseEntity<Map<String, Object>> syncVectors() {
        try {
            vectorSyncService.syncAllVectors();
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "向量同步成功");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "向量同步失败: " + e.getMessage());
            return ResponseEntity.status(500).body(result);
        }
    }
}
