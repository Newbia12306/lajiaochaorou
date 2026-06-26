package com.example.demo.service.search;

import com.example.demo.model.Dish;
import com.example.demo.repository.DishRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DatabaseSearchService {

    @Autowired
    private DishRepository dishRepository;

    public List<Dish> search(String keywords, int limit) {
        if (keywords == null || keywords.trim().isEmpty()) {
            return Collections.emptyList();
        }

        List<Dish> allDishes = dishRepository.findByIsDeletedFalse();
        
        String lowerKeywords = keywords.toLowerCase().trim();
        
        return allDishes.stream()
                .filter(dish -> {
                    String dishName = dish.getName().toLowerCase();
                    return dishName.contains(lowerKeywords);
                })
                .limit(limit)
                .collect(Collectors.toList());
    }

    public boolean isAvailable() {
        return true;
    }

    public String getEngineName() {
        return "数据库搜索";
    }
}
