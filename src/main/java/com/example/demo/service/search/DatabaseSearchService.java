package com.example.demo.service.search;

import com.example.demo.model.Dish;
import com.example.demo.repository.DishRepository;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class DatabaseSearchService {

    private final DishRepository dishRepository;

    public DatabaseSearchService(DishRepository dishRepository) {
        this.dishRepository = dishRepository;
    }

    public List<Dish> search(String keywords, int limit) {
        if (keywords == null || keywords.trim().isEmpty()) {
            return Collections.emptyList();
        }

        List<Dish> allDishes = dishRepository.findByIsDeletedFalse();
        String lowerKeywords = keywords.toLowerCase().trim();

        return allDishes.stream()
                .filter(dish -> dish.getName().toLowerCase().contains(lowerKeywords))
                .limit(limit)
                .toList();
    }

    public boolean isAvailable() {
        return true;
    }

    public String getEngineName() {
        return "数据库搜索";
    }
}
