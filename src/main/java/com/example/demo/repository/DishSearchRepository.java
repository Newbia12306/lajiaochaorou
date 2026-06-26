package com.example.demo.repository;

import com.example.demo.model.DishDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DishSearchRepository extends ElasticsearchRepository<DishDocument, String> {
}
