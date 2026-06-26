package com.example.demo.service;

import com.example.demo.model.Dish;
import com.example.demo.repository.DishRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class VectorSyncService {

    @Autowired
    private DishRepository dishRepository;

    @Autowired
    private EmbeddingService embeddingService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public void syncAllVectors() {
        List<Dish> dishes = dishRepository.findByIsDeletedFalse();
        System.out.println("开始同步 " + dishes.size() + " 道菜品的向量...");

        int count = 0;
        for (Dish dish : dishes) {
            try {
                float[] embedding = embeddingService.generateEmbedding(dish.getName());
                
                String sql = "UPDATE menu SET embedding = ?::vector WHERE id = ?";
                
                Float[] floatArray = new Float[embedding.length];
                for (int i = 0; i < embedding.length; i++) {
                    floatArray[i] = embedding[i];
                }
                
                jdbcTemplate.update(sql, floatArray, dish.getId());
                
                count++;
                if (count % 10 == 0) {
                    System.out.println("已同步 " + count + " 条...");
                }
            } catch (Exception e) {
                System.err.println("同步菜品 " + dish.getId() + " 失败: " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        System.out.println("向量同步完成，共 " + count + " 条");
    }
}
