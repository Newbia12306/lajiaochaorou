package com.example.demo.service;

import com.example.demo.model.Dish;
import com.example.demo.repository.DishRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class DishService {

    private static final Logger log = LoggerFactory.getLogger(DishService.class);
    private static final String SIGNATURE_DISHES_CACHE_KEY = "signature_dishes";
    private static final long CACHE_EXPIRE_TIME = 30;

    private final DishRepository dishRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    public DishService(DishRepository dishRepository, RedisTemplate<String, Object> redisTemplate) {
        this.dishRepository = dishRepository;
        this.redisTemplate = redisTemplate;
    }

    public void validateDishName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("菜品名称不能为空");
        }
        name = name.trim();

        if (name.length() < 4 || name.length() > 20) {
            throw new IllegalArgumentException("菜品名称长度必须在4到20个字符之间");
        }
        if (name.matches(".*\\d.*")) {
            throw new IllegalArgumentException("菜品不能包含数字");
        }
    }

    public Dish createDish(String name, Dish.SpicyLevel spicy, Boolean isSignature) {
        validateDishName(name);
        if (dishRepository.existsByName(name)) {
            throw new IllegalArgumentException("菜品已添加");
        }
        Dish dish = new Dish();
        dish.setName(name.trim());
        dish.setSpicy(spicy);
        dish.setIsSignature(isSignature != null ? isSignature : false);

        Dish savedDish = dishRepository.save(dish);

        if (Boolean.TRUE.equals(isSignature)) {
            clearSignatureDishesCache();
        }

        return savedDish;
    }

    public List<Dish> getAllDishes() {
        return dishRepository.findByIsDeletedFalse();
    }

    /**
     * Get signature dishes with Redis caching.
     */
    @SuppressWarnings("unchecked")
    public List<Dish> getSignatureDishes() {
        Object cached = redisTemplate.opsForValue().get(SIGNATURE_DISHES_CACHE_KEY);
        if (cached instanceof List<?> list && !list.isEmpty() && list.get(0) instanceof Dish) {
            return (List<Dish>) list;
        }

        List<Dish> signatureDishes = dishRepository.findByIsSignatureTrueAndIsDeletedFalse();

        if (signatureDishes != null && !signatureDishes.isEmpty()) {
            redisTemplate.opsForValue().set(SIGNATURE_DISHES_CACHE_KEY, signatureDishes,
                    CACHE_EXPIRE_TIME, TimeUnit.MINUTES);
        }

        return signatureDishes != null ? signatureDishes : Collections.emptyList();
    }

    public Map<String, Object> getDishesWithPagination(String sortType, int page, int size) {
        if (page < 1) page = 1;
        if (size < 1) size = 10;
        if (size > 100) size = 100;

        Page<Dish> dishPage;
        Pageable pageable = PageRequest.of(page - 1, size);

        switch (sortType) {
            case "spicy":
                dishPage = dishRepository.findAllOrderBySpicyLevel(pageable);
                break;
            case "signature":
                dishPage = dishRepository.findAllOrderBySignature(pageable);
                break;
            default:
                dishPage = dishRepository.findAll(pageable);
                break;
        }
        Map<String, Object> response = new HashMap<>();
        response.put("dishes", dishPage.getContent());
        response.put("currentPage", page);
        response.put("totalPages", dishPage.getTotalPages());
        response.put("totalItems", dishPage.getTotalElements());
        response.put("hasNext", dishPage.hasNext());
        response.put("hasPrevious", dishPage.hasPrevious());
        response.put("pageSize", size);

        return response;
    }

    public void deleteDish(Long id) {
        Dish dish = dishRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("菜品不存在"));

        boolean wasSignature = Boolean.TRUE.equals(dish.getIsSignature());
        dish.setIsDeleted(true);
        dishRepository.save(dish);

        if (wasSignature) {
            clearSignatureDishesCache();
        }
    }

    /**
     * Batch delete dishes - uses findAllById to avoid N+1 queries.
     */
    public void deleteDishes(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new IllegalArgumentException("没有选中可删除的菜品");
        }

        // Fix N+1: fetch all dishes in one query
        List<Dish> dishes = dishRepository.findAllById(ids);
        boolean hasSignatureDish = false;

        for (Dish dish : dishes) {
            if (Boolean.TRUE.equals(dish.getIsSignature())) {
                hasSignatureDish = true;
            }
            dish.setIsDeleted(true);
        }
        dishRepository.saveAll(dishes);

        if (hasSignatureDish) {
            clearSignatureDishesCache();
        }
    }

    public Dish getDishById(Long id) {
        return dishRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("菜品不存在，ID: " + id));
    }

    public Dish updateDish(Long id, Dish dishDetails) {
        Dish dish = getDishById(id);

        if (dishDetails.getName() != null
                && !dish.getName().equals(dishDetails.getName())
                && dishRepository.existsByName(dishDetails.getName())) {
            throw new IllegalArgumentException("菜品名称已存在: " + dishDetails.getName());
        }

        boolean signatureChanged = dishDetails.getIsSignature() != null
                && !dish.getIsSignature().equals(dishDetails.getIsSignature());

        if (dishDetails.getName() != null) {
            dish.setName(dishDetails.getName());
        }
        if (dishDetails.getSpicy() != null) {
            dish.setSpicy(dishDetails.getSpicy());
        }
        if (dishDetails.getIsSignature() != null) {
            dish.setIsSignature(dishDetails.getIsSignature());
        }

        Dish updatedDish = dishRepository.save(dish);

        if (signatureChanged) {
            clearSignatureDishesCache();
        }

        return updatedDish;
    }

    private void clearSignatureDishesCache() {
        redisTemplate.delete(SIGNATURE_DISHES_CACHE_KEY);
    }

    /**
     * Search dishes by name keyword with caching.
     */
    public List<Dish> searchByNameKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return Collections.emptyList();
        }
        String cacheKey = "dish_search:" + keyword.toLowerCase();
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached instanceof List<?> list && !list.isEmpty() && list.get(0) instanceof Dish) {
            @SuppressWarnings("unchecked")
            List<Dish> result = (List<Dish>) list;
            return result;
        }

        List<Dish> dishes = dishRepository.findByNameContainingIgnoreCase(keyword);
        if (dishes != null && !dishes.isEmpty()) {
            redisTemplate.opsForValue().set(cacheKey, dishes, 10, TimeUnit.MINUTES);
        }
        return dishes != null ? dishes : Collections.emptyList();
    }
}
