package com.example.demo.DTO;

import java.util.List;

public class CreateOrderRequest {

    private String userId;
    private List<OrderItem> items;

    public static class OrderItem {
        private Integer dishId;
        private Integer quantity = 1;

        public OrderItem() {}

        public Integer getDishId() { return dishId; }
        public void setDishId(Integer dishId) { this.dishId = dishId; }

        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }
    }

    public CreateOrderRequest() {}

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public List<OrderItem> getItems() { return items; }
    public void setItems(List<OrderItem> items) { this.items = items; }
}
