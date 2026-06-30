package com.example.demo.controller;

import com.example.demo.service.LlmService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/llm")
public class LlmController {

    private final LlmService llmService;

    public LlmController(LlmService llmService) {
        this.llmService = llmService;
    }

    @PostMapping("/chat")
    public ResponseEntity<Map<String, Object>> chat(@RequestBody Map<String, String> request) {
        String message = request.getOrDefault("message", "");

        Map<String, Object> chatResult = llmService.chat(message);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("response", chatResult.get("response"));
        result.put("dishes", chatResult.get("dishes"));
        return ResponseEntity.ok(result);
    }

    @PostMapping("/recommend")
    public ResponseEntity<Map<String, Object>> smartRecommend(@RequestBody Map<String, String> request) {
        String tastePreference = request.get("tastePreference");
        String dietaryRestrictions = request.get("dietaryRestrictions");

        String recommendation = llmService.recommendDishes(tastePreference, dietaryRestrictions);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("recommendation", recommendation);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> result = new HashMap<>();
        result.put("llmConfigured", true);
        result.put("message", "LLM 服务正常运行");
        return ResponseEntity.ok(result);
    }
}
