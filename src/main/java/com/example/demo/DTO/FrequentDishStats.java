package com.example.demo.dto;

import java.time.LocalDateTime;

/**
 * 用户常点菜品统计结果
 */
public class FrequentDishStats {

    private Integer dishId;
    private String dishName;
    private Long count;
    private LocalDateTime lastOrderedAt;

    public FrequentDishStats(Integer dishId, String dishName, Long count, LocalDateTime lastOrderedAt) {
        this.dishId = dishId;
        this.dishName = dishName;
        this.count = count;
        this.lastOrderedAt = lastOrderedAt;
    }

    public Integer getDishId() { return dishId; }
    public void setDishId(Integer dishId) { this.dishId = dishId; }

    public String getDishName() { return dishName; }
    public void setDishName(String dishName) { this.dishName = dishName; }

    public Long getCount() { return count; }
    public void setCount(Long count) { this.count = count; }

    public LocalDateTime getLastOrderedAt() { return lastOrderedAt; }
    public void setLastOrderedAt(LocalDateTime lastOrderedAt) { this.lastOrderedAt = lastOrderedAt; }
}
