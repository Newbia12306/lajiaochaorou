package com.example.demo.service;

import com.example.demo.model.Dish;
import com.example.demo.repository.DishRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class VectorSyncService {

    private static final Logger log = LoggerFactory.getLogger(VectorSyncService.class);

    private final DishRepository dishRepository;
    private final EmbeddingService embeddingService;
    private final JdbcTemplate jdbcTemplate;

    public VectorSyncService(DishRepository dishRepository,
                              EmbeddingService embeddingService,
                              JdbcTemplate jdbcTemplate) {
        this.dishRepository = dishRepository;
        this.embeddingService = embeddingService;
        this.jdbcTemplate = jdbcTemplate;
    }

    public void syncAllVectors() {
        List<Dish> dishes = dishRepository.findByIsDeletedFalse();
        log.info("开始同步 {} 道菜品的向量...", dishes.size());

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
                    log.info("已同步 {} 条...", count);
                }
            } catch (Exception e) {
                log.error("同步菜品 {} 失败: {}", dish.getId(), e.getMessage(), e);
            }
        }

        log.info("向量同步完成，共 {} 条", count);
    }
}
