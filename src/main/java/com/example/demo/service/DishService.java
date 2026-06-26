package com.example.demo.service;

import com.example.demo.model.Dish;
import com.example.demo.repository.DishRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.data.redis.core.RedisTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.Collections;

@Service
public class DishService {

    @Autowired
    private DishRepository dishRepository;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final String SIGNATURE_DISHES_CACHE_KEY = "signature_dishes";
    private static final long CACHE_EXPIRE_TIME = 30; // 缓存过期时间（分钟）

    // 验证菜品名称
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

    public Dish createDish(String name,Dish.SpicyLevel spicy ,Boolean isSignature){
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

    public List<Dish> getAllDishes(){
        return dishRepository.findByIsDeletedFalse();
    }

    /**
     * 获取招牌菜列表（带缓存）
     */
    @SuppressWarnings("unchecked")
    public List<Dish> getSignatureDishes() {
        // 先从缓存中获取
        List<Dish> cachedDishes = (List<Dish>) redisTemplate.opsForValue().get(SIGNATURE_DISHES_CACHE_KEY);
        if (cachedDishes != null) {
            return cachedDishes;
        }

        // 缓存中没有，从数据库查询
        List<Dish> signatureDishes = dishRepository.findByIsSignatureTrueAndIsDeletedFalse();
        
        // 将结果存入缓存
        if (signatureDishes != null && !signatureDishes.isEmpty()) {
            redisTemplate.opsForValue().set(SIGNATURE_DISHES_CACHE_KEY, signatureDishes, CACHE_EXPIRE_TIME, TimeUnit.MINUTES);
        }
        
        return signatureDishes != null ? signatureDishes : Collections.emptyList();
    }

    public Map<String,Object> getDishesWithPagination(String sortType,int page,int size){
        if(page < 1) page = 1;
        if(size < 1) size = 10;
        if(size > 100) size = 100;

        Page<Dish> dishPage;
        Pageable pageable = PageRequest.of(page - 1 , size);

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
        Map<String,Object> response = new HashMap<>();
        response.put("dishes",dishPage.getContent());
        response.put("currentPage",page);
        response.put("totalPages",dishPage.getTotalPages());
        response.put("totalItems",dishPage.getTotalElements());
        response.put("hasNext",dishPage.hasNext());
        response.put("hasPrevious",dishPage.hasPrevious());
        response.put("pageSize",size);

        return response;
    }

    public void deleteDish(Long id){
        Dish dish = dishRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("菜品不存在"));

        boolean wasSignature = Boolean.TRUE.equals(dish.getIsSignature());

        dish.setIsDeleted(true);
        dishRepository.save(dish);

        if (wasSignature) {
            clearSignatureDishesCache();
        }
    }

    public void deleteDishes(List<Long> ids){
        if (ids == null || ids.isEmpty()) {
            throw new IllegalArgumentException("没有选中可删除的菜品");
        }
        
        boolean hasSignatureDish = false;
        for(Long id : ids){
            Dish dish = dishRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("菜品不存在"));
            if (Boolean.TRUE.equals(dish.getIsSignature())) {
                hasSignatureDish = true;
            }
            dish.setIsDeleted(true);
            dishRepository.save(dish);
        }
        
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

        if (!dish.getName().equals(dishDetails.getName()) &&
                dishRepository.existsByName(dishDetails.getName())) {
            throw new RuntimeException("菜品名称已存在: " + dishDetails.getName());
        }

        boolean signatureChanged = !dish.getIsSignature().equals(dishDetails.getIsSignature());

        dish.setName(dishDetails.getName());
        dish.setSpicy(dishDetails.getSpicy());
        dish.setIsSignature(dishDetails.getIsSignature());

        Dish updatedDish = dishRepository.save(dish);
        
        if (signatureChanged) {
            clearSignatureDishesCache();
        }
        
        return updatedDish;
    }

    /**
     * 清除招牌菜缓存
     */
    private void clearSignatureDishesCache() {
        redisTemplate.delete(SIGNATURE_DISHES_CACHE_KEY);
    }
}
