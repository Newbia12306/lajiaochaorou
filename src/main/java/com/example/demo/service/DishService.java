package com.example.demo.service;

import com.example.demo.model.Dish;
import com.example.demo.repository.DishRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.HashMap;
import java.util.Map;

@Service
public class DishService {

    @Autowired
    private DishRepository dishRepository;

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

        return dishRepository.save(dish);
    }

    public List<Dish> getAllDishes(){
        return dishRepository.findByIsDeletedFalse();
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

        dish.setIsDeleted(true);
        dishRepository.save(dish);
    }

    public void deleteDishes(List<Long> ids){
        if (ids == null || ids.isEmpty()) {
            throw new IllegalArgumentException("没有选中可删除的菜品");
        }
        for(Long id : ids){
            deleteDish(id);
        }
    }
    public Dish getDishById(Long id) {
        return dishRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("菜品不存在，ID: " + id));
    }
    public Dish updateDish(Long id, Dish dishDetails) {
        Dish dish = getDishById(id);

        // 如果修改了名称，检查新名称是否与其他菜品重复
        if (!dish.getName().equals(dishDetails.getName()) &&
                dishRepository.existsByName(dishDetails.getName())) {
            throw new RuntimeException("菜品名称已存在: " + dishDetails.getName());
        }

        // 更新字段
        dish.setName(dishDetails.getName());
        dish.setSpicy(dishDetails.getSpicy());
        dish.setIsSignature(dishDetails.getIsSignature());

        return dishRepository.save(dish);
    }
}
