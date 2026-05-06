package com.example.demo.controller;

import com.example.demo.model.Dish;
import com.example.demo.repository.DishRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.example.demo.service.DishService;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/dishes")
@CrossOrigin(origins = "*")

public class DishController {

    @Autowired
    private  DishService dishService;

    @GetMapping
    public ResponseEntity<?> getDishesWithPagination(
            @RequestParam(defaultValue = "default") String sort,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ){
        try{
            Map<String,Object> result = dishService.getDishesWithPagination(sort, page, size);
            return ResponseEntity.ok(result);
        }catch (Exception e){
            Map<String,Object> errorResponse = Map.of(
                    "error",true,
                    "message","获取菜品失败" + e.getMessage()
            );
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    @GetMapping("/all")
    public ResponseEntity<?> getAllDishes(){
        try{
            List<Dish> dishes = dishService.getAllDishes();
            Map<String,Object> response = new HashMap<>();
            response.put("data",dishes);
            response.put("total",dishes.size());
            return ResponseEntity.ok(response);
        }catch (Exception e){
            Map<String,Object> errorResponse = Map.of(
                    "error",true,
                    "message","获取菜品失败" + e.getMessage()
            );
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }



    @PostMapping
    public ResponseEntity<String> create(@RequestBody Map<String,Object> request) {
        try {
            String name = (String) request.get("name");
            name = name.trim();

            Dish.SpicyLevel spicy = null;
            if (request.get("spicy") != null) {
                try {
                    spicy = Dish.SpicyLevel.valueOf(((String) request.get("spicy")).toUpperCase());
                } catch (IllegalArgumentException e) {
                    return ResponseEntity.badRequest().body("输入无效");
                }
            }
            Boolean isSignature = null;
            if (request.get("isSignature") != null) {
                if (request.get("isSignature") instanceof Boolean) {
                    isSignature = (Boolean) request.get("isSignature");
                } else if (request.get("isSignature") instanceof String) {
                    isSignature = Boolean.parseBoolean((String) request.get("isSignature"));
                }
            }
            Dish dish = dishService.createDish(name, spicy, isSignature);
            return ResponseEntity.ok("菜品添加成功");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("添加失败: " + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Dish> getDishById(@PathVariable Long id){
        try{
            Dish dish = dishService.getDishById(id);
            return ResponseEntity.ok(dish);
        }catch(IllegalArgumentException e){
            return ResponseEntity.notFound().build();
        }
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteDish(@PathVariable Long id) {
        try {
            dishService.deleteDish(id);
            return ResponseEntity.ok("菜品删除成功");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("删除失败: " + e.getMessage());
        }
    }
    @PostMapping("/batch-delete")
    public ResponseEntity<String> batchDeleteDishes(@RequestBody Map<String,List<Long>> request){
        try{
            List<Long> ids = request.get("ids");
            dishService.deleteDishes(ids);
            return ResponseEntity.ok("成功删除" + ids.size() + "个菜品");
        }catch(IllegalArgumentException e){
            return ResponseEntity.badRequest().body(e.getMessage());
        }catch (Exception e){
            return ResponseEntity.internalServerError().body("批量删除失败");
        }
    }
    @GetMapping("/spicy-levels")
    public ResponseEntity<Dish.SpicyLevel[]> getSpicyLevels() {
        try {
            Dish.SpicyLevel[] spicyLevels = Dish.SpicyLevel.values();
            return ResponseEntity.ok(spicyLevels);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
    }
    @PutMapping("/{id}")
    public ResponseEntity<?> updateDish(
            @PathVariable Long id,
            @RequestBody Map<String, Object> request) {

        try {
            // 从 Map 中获取参数
            String name = (String) request.get("name");
            String spicyLevelStr = (String) request.get("spicy");
            Boolean isSignature = (Boolean) request.get("isSignature");

            // 创建 Dish 对象
            Dish dishDetails = new Dish();
            dishDetails.setName(name);

            if (spicyLevelStr != null) {
                Dish.SpicyLevel spicyLevel = Dish.SpicyLevel.valueOf(spicyLevelStr.toUpperCase());
                dishDetails.setSpicy(spicyLevel);
            }

            if (isSignature != null) {
                dishDetails.setIsSignature(isSignature);
            }

            Dish updatedDish = dishService.updateDish(id, dishDetails);
            return ResponseEntity.ok(updatedDish);

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }




}
