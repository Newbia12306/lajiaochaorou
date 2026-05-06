package com.example.demo.controller;

import com.example.demo.DTO.RecommendationResponse;
import com.example.demo.service.RecommendationService;
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

    @PostMapping("/search")
    public ResponseEntity<RecommendationResponse> searchByTaste(
            @RequestBody Map<String, Object> request) {
        String tasteKeywords = (String) request.get("tasteKeywords");
        Integer limit = request.get("limit") != null ? 
                ((Number) request.get("limit")).intValue() : 10;
        
        RecommendationResponse response = recommendationService.recommendByTaste(tasteKeywords, limit);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/search/simple")
    public ResponseEntity<RecommendationResponse> searchByTasteSimple(
            @RequestParam String keywords,
            @RequestParam(defaultValue = "10") Integer limit) {
        RecommendationResponse response = recommendationService.recommendByTaste(keywords, limit);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/rebuild-index")
    public ResponseEntity<Map<String, Object>> rebuildIndex() {
        recommendationService.rebuildIndex();
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "索引重建成功");
        return ResponseEntity.ok(result);
    }
}
