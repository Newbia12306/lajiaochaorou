package com.example.demo.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "order_records")
public class OrderRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "dish_id", nullable = false)
    private Integer dishId;

    @Column(name = "order_id", nullable = false)
    private String orderId;

    @Column(name = "quantity", nullable = false)
    private Integer quantity = 1;

    @Column(name = "ordered_at", nullable = false)
    private LocalDateTime orderedAt;

    public OrderRecord() {}

    public OrderRecord(String userId, Integer dishId, String orderId, Integer quantity, LocalDateTime orderedAt) {
        this.userId = userId;
        this.dishId = dishId;
        this.orderId = orderId;
        this.quantity = quantity;
        this.orderedAt = orderedAt;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public Integer getDishId() { return dishId; }
    public void setDishId(Integer dishId) { this.dishId = dishId; }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public LocalDateTime getOrderedAt() { return orderedAt; }
    public void setOrderedAt(LocalDateTime orderedAt) { this.orderedAt = orderedAt; }
}
