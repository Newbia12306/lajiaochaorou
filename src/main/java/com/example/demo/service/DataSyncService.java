package com.example.demo.service;

import com.example.demo.model.Dish;
import com.example.demo.model.DishDocument;
import com.example.demo.repository.DishRepository;
import com.example.demo.repository.DishSearchRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DataSyncService {

    private static final Logger log = LoggerFactory.getLogger(DataSyncService.class);

    private final DishRepository dishRepository;
    private final DishSearchRepository dishSearchRepository;

    public DataSyncService(DishRepository dishRepository,
                           DishSearchRepository dishSearchRepository) {
        this.dishRepository = dishRepository;
        this.dishSearchRepository = dishSearchRepository;
    }

    public void syncAllDishesToElasticsearch() {
        log.info("开始同步菜品数据到 Elasticsearch...");

        try {
            List<Dish> dishes = dishRepository.findByIsDeletedFalse();

            if (dishes.isEmpty()) {
                log.info("没有需要同步的菜品数据");
                return;
            }

            List<DishDocument> dishDocuments = dishes.stream()
                    .map(DishDocument::new)
                    .toList();

            dishSearchRepository.deleteAll();
            dishSearchRepository.saveAll(dishDocuments);

            log.info("成功同步 {} 条菜品数据到 Elasticsearch", dishDocuments.size());
        } catch (DataAccessException e) {
            log.error("同步菜品数据到 Elasticsearch 失败", e);
            throw new RuntimeException("数据同步失败: " + e.getMessage(), e);
        }
    }
}
