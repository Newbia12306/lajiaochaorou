package com.example.demo.controller;

import com.example.demo.DTO.CreateOrderRequest;
import com.example.demo.DTO.FrequentDishStats;
import com.example.demo.service.LlmService;
import com.example.demo.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "*")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private LlmService llmService;

    /**
     * 创建订单
     */
    @PostMapping
    public ResponseEntity<?> createOrder(@RequestBody CreateOrderRequest request) {
        try {
            Map<String, Object> result = orderService.createOrder(request);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", true, "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", true, "message", "下单失败: " + e.getMessage()));
        }
    }

    /**
     * 查询用户历史订单
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getUserOrders(
            @PathVariable String userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Map<String, Object> result = orderService.getUserOrders(userId, page, size);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", true, "message", "查询订单失败: " + e.getMessage()));
        }
    }

    /**
     * 查询用户最常点的菜品（统计接口）
     */
    @GetMapping("/frequent")
    public ResponseEntity<?> getFrequentDishes(
            @RequestParam String userId,
            @RequestParam(defaultValue = "5") int limit) {
        try {
            List<FrequentDishStats> frequentDishes = orderService.getFrequentDishes(userId, limit);
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("userId", userId);
            result.put("frequentDishes", frequentDishes);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", true, "message", "查询常点菜品失败: " + e.getMessage()));
        }
    }

    /**
     * LLM 个性化推荐：结合用户历史点单 + 全菜单，用 DeepSeek 生成推荐
     */
    @PostMapping("/recommendations")
    public ResponseEntity<?> getPersonalizedRecommendations(@RequestBody Map<String, Object> request) {
        try {
            String userId = (String) request.get("userId");

            if (userId == null || userId.isBlank()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", true, "message", "用户ID不能为空"));
            }

            String recommendation = llmService.personalizedRecommend(userId);

            // 同时返回用户的常点菜品，方便前端展示
            List<FrequentDishStats> frequentDishes = orderService.getFrequentDishes(userId, 5);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("userId", userId);
            result.put("recommendation", recommendation);
            result.put("frequentDishes", frequentDishes);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", true, "message", "生成推荐失败: " + e.getMessage()));
        }
    }
}
