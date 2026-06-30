package com.example.demo.service;

import com.example.demo.constants.ApiConstants;
import com.example.demo.dto.CreateOrderRequest;
import com.example.demo.dto.FrequentDishStats;
import com.example.demo.model.OrderRecord;
import com.example.demo.repository.OrderRecordRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class OrderService {

    private final OrderRecordRepository orderRecordRepository;

    public OrderService(OrderRecordRepository orderRecordRepository) {
        this.orderRecordRepository = orderRecordRepository;
    }

    public Map<String, Object> createOrder(CreateOrderRequest request) {
        if (request.getUserId() == null || request.getUserId().isBlank()) {
            throw new IllegalArgumentException("用户ID不能为空");
        }
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new IllegalArgumentException("订单中至少需要一个菜品");
        }

        String orderId = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();
        List<OrderRecord> records = new ArrayList<>();
        int totalQuantity = 0;

        for (CreateOrderRequest.OrderItem item : request.getItems()) {
            if (item.getDishId() == null) {
                throw new IllegalArgumentException("菜品ID不能为空");
            }
            int qty = item.getQuantity() != null && item.getQuantity() > 0 ? item.getQuantity() : 1;
            records.add(new OrderRecord(request.getUserId(), item.getDishId(), orderId, qty, now));
            totalQuantity += qty;
        }

        orderRecordRepository.saveAll(records);

        Map<String, Object> result = new HashMap<>();
        result.put(ApiConstants.KEY_SUCCESS, true);
        result.put(ApiConstants.KEY_MESSAGE, "下单成功");
        result.put("orderId", orderId);
        result.put("totalItems", records.size());
        result.put("totalQuantity", totalQuantity);
        return result;
    }

    public Map<String, Object> getUserOrders(String userId, int page, int size) {
        Page<OrderRecord> orderPage = orderRecordRepository
                .findByUserIdOrderByOrderedAtDesc(userId, PageRequest.of(page - 1, size));

        Map<String, Object> result = new HashMap<>();
        result.put("data", orderPage.getContent());
        result.put("total", orderPage.getTotalElements());
        result.put("page", page);
        result.put("size", size);
        result.put("totalPages", orderPage.getTotalPages());
        return result;
    }

    public List<FrequentDishStats> getFrequentDishes(String userId, int limit) {
        return orderRecordRepository.findFrequentDishesByUserId(userId, PageRequest.of(0, limit));
    }
}
