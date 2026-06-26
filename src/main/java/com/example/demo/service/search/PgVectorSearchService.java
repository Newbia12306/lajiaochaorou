package com.example.demo.service.search;

import com.example.demo.model.Dish;
import com.example.demo.service.EmbeddingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Array;
import java.util.Collections;
import java.util.List;

@Service
public class PgVectorSearchService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EmbeddingService embeddingService;

    public List<Dish> search(String keywords, int limit) {
        if (keywords == null || keywords.trim().isEmpty()) {
            return Collections.emptyList();
        }

        try {
            float[] queryEmbedding = embeddingService.generateEmbedding(keywords);
            
            String sql = "SELECT id, name, spicy, is_signature FROM menu " +
                        "WHERE is_deleted = false " +
                        "ORDER BY embedding <-> ?::vector " +
                        "LIMIT ?";

            Float[] floatArray = new Float[queryEmbedding.length];
            for (int i = 0; i < queryEmbedding.length; i++) {
                floatArray[i] = queryEmbedding[i];
            }

            return jdbcTemplate.query(
                conn -> {
                    var ps = conn.prepareStatement(sql);
                    Array sqlArray = conn.createArrayOf("float", floatArray);
                    ps.setArray(1, sqlArray);
                    ps.setInt(2, limit);
                    return ps;
                },
                (rs, rowNum) -> {
                    Dish dish = new Dish();
                    dish.setId(rs.getInt("id"));
                    dish.setName(rs.getString("name"));
                    String spicyStr = rs.getString("spicy");
                    if (spicyStr != null) {
                        try {
                            dish.setSpicy(Dish.SpicyLevel.valueOf(spicyStr));
                        } catch (IllegalArgumentException e) {
                            dish.setSpicy(null);
                        }
                    }
                    dish.setIsSignature(rs.getBoolean("is_signature"));
                    dish.setIsDeleted(false);
                    return dish;
                });

        } catch (Exception e) {
            System.err.println("PG Vector搜索失败: " + e.getMessage());
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    public boolean isAvailable() {
        try {
            jdbcTemplate.queryForObject("SELECT 1 FROM information_schema.tables WHERE table_name = 'menu' LIMIT 1", Integer.class);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public String getEngineName() {
        return "PG Vector搜索";
    }
}
