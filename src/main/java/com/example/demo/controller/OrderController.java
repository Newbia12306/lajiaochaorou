package com.example.demo.controller;

import com.example.demo.DTO.CreateOrderRequest;
import com.example.demo.DTO.FrequentDishStats;
import com.example.demo.service.LlmService;
import com.example.demo.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private static final Logger log = LoggerFactory.getLogger(OrderController.class);
    private final OrderService orderService;
    private final LlmService llmService;

    public OrderController(OrderService orderService, LlmService llmService) {
        this.orderService = orderService;
        this.llmService = llmService;
    }

    @PostMapping
    public ResponseEntity<?> createOrder(@RequestBody CreateOrderRequest request) {
        try {
            Map<String, Object> result = orderService.createOrder(request);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", true, "message", e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to create order", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", true, "message", "下单失败: " + e.getMessage()));
        }
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getUserOrders(
            @PathVariable String userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Map<String, Object> result = orderService.getUserOrders(userId, page, size);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Failed to get orders for userId={}", userId, e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", true, "message", "查询订单失败: " + e.getMessage()));
        }
    }

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
            log.error("Failed to get frequent dishes for userId={}", userId, e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", true, "message", "查询常点菜品失败: " + e.getMessage()));
        }
    }

    @PostMapping("/recommendations")
    public ResponseEntity<?> getPersonalizedRecommendations(@RequestBody Map<String, Object> request) {
        try {
            String userId = (String) request.get("userId");

            if (userId == null || userId.isBlank()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", true, "message", "用户ID不能为空"));
            }

            String recommendation = llmService.personalizedRecommend(userId);

            List<FrequentDishStats> frequentDishes = orderService.getFrequentDishes(userId, 5);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("userId", userId);
            result.put("recommendation", recommendation);
            result.put("frequentDishes", frequentDishes);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Failed to generate recommendations", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", true, "message", "生成推荐失败: " + e.getMessage()));
        }
    }
}
