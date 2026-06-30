package com.example.demo.controller;

import com.example.demo.constants.ApiConstants;
import com.example.demo.model.Dish;
import com.example.demo.service.DishService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dishes")
public class DishController {

    private static final Logger log = LoggerFactory.getLogger(DishController.class);
    private final DishService dishService;

    public DishController(DishService dishService) {
        this.dishService = dishService;
    }

    @GetMapping
    public ResponseEntity<?> getDishesWithPagination(
            @RequestParam(defaultValue = "default") String sort,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Map<String, Object> result = dishService.getDishesWithPagination(sort, page, size);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Failed to get dishes with pagination", e);
            return ResponseEntity.internalServerError().body(
                    Map.of(ApiConstants.KEY_ERROR, true, ApiConstants.KEY_MESSAGE, "获取菜品失败: " + e.getMessage()));
        }
    }

    @GetMapping("/all")
    public ResponseEntity<?> getAllDishes() {
        try {
            List<Dish> dishes = dishService.getAllDishes();
            Map<String, Object> response = new HashMap<>();
            response.put("data", dishes);
            response.put("total", dishes.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to get all dishes", e);
            return ResponseEntity.internalServerError().body(
                    Map.of(ApiConstants.KEY_ERROR, true, ApiConstants.KEY_MESSAGE, "获取菜品失败: " + e.getMessage()));
        }
    }

    @GetMapping("/signature")
    public ResponseEntity<?> getSignatureDishes() {
        try {
            List<Dish> signatureDishes = dishService.getSignatureDishes();
            Map<String, Object> response = new HashMap<>();
            response.put("data", signatureDishes);
            response.put("total", signatureDishes.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to get signature dishes", e);
            return ResponseEntity.internalServerError().body(
                    Map.of(ApiConstants.KEY_ERROR, true, ApiConstants.KEY_MESSAGE, "获取招牌菜失败: " + e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<String> create(@RequestBody Map<String, Object> request) {
        try {
            String name = (String) request.get("name");
            if (name == null || name.isBlank()) {
                return ResponseEntity.badRequest().body("菜品名称不能为空");
            }
            name = name.trim();

            Dish.SpicyLevel spicy = null;
            if (request.get("spicy") != null) {
                try {
                    spicy = Dish.SpicyLevel.valueOf(((String) request.get("spicy")).toUpperCase());
                } catch (IllegalArgumentException e) {
                    return ResponseEntity.badRequest().body("无效的辣度值");
                }
            }

            Boolean isSignature = null;
            if (request.get("isSignature") != null) {
                if (request.get("isSignature") instanceof Boolean b) {
                    isSignature = b;
                } else if (request.get("isSignature") instanceof String s) {
                    isSignature = Boolean.parseBoolean(s);
                }
            }

            dishService.createDish(name, spicy, isSignature);
            return ResponseEntity.ok("菜品添加成功");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("Failed to create dish", e);
            return ResponseEntity.badRequest().body("添加失败: " + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Dish> getDishById(@PathVariable Long id) {
        try {
            Dish dish = dishService.getDishById(id);
            return ResponseEntity.ok(dish);
        } catch (IllegalArgumentException e) {
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
            log.error("Failed to delete dish id={}", id, e);
            return ResponseEntity.badRequest().body("删除失败: " + e.getMessage());
        }
    }

    @PostMapping("/batch-delete")
    public ResponseEntity<String> batchDeleteDishes(@RequestBody Map<String, List<Long>> request) {
        try {
            List<Long> ids = request.get("ids");
            if (ids == null || ids.isEmpty()) {
                return ResponseEntity.badRequest().body("ID列表不能为空");
            }
            dishService.deleteDishes(ids);
            return ResponseEntity.ok("成功删除" + ids.size() + "个菜品");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("Failed to batch delete dishes", e);
            return ResponseEntity.internalServerError().body("批量删除失败");
        }
    }

    @GetMapping("/spicy-levels")
    public ResponseEntity<Dish.SpicyLevel[]> getSpicyLevels() {
        return ResponseEntity.ok(Dish.SpicyLevel.values());
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateDish(
            @PathVariable Long id,
            @RequestBody Map<String, Object> request) {

        try {
            String name = (String) request.get("name");
            if (name != null) {
                name = name.trim();
            }
            String spicyLevelStr = (String) request.get("spicy");
            Boolean isSignature = request.get("isSignature") instanceof Boolean b ? b : null;

            Dish dishDetails = new Dish();
            dishDetails.setName(name);

            if (spicyLevelStr != null && !spicyLevelStr.isBlank()) {
                dishDetails.setSpicy(Dish.SpicyLevel.valueOf(spicyLevelStr.toUpperCase()));
            }

            if (isSignature != null) {
                dishDetails.setIsSignature(isSignature);
            }

            Dish updatedDish = dishService.updateDish(id, dishDetails);
            return ResponseEntity.ok(updatedDish);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(ApiConstants.KEY_ERROR, e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to update dish id={}", id, e);
            return ResponseEntity.internalServerError().body(Map.of(ApiConstants.KEY_ERROR, e.getMessage()));
        }
    }
}
