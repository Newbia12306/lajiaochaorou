package com.example.demo.service.search;

import com.example.demo.model.Dish;
import com.example.demo.model.DishDocument;
import com.example.demo.repository.DishSearchRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class ElasticsearchSearchService {

    private static final Logger log = LoggerFactory.getLogger(ElasticsearchSearchService.class);

    private final ElasticsearchOperations elasticsearchOperations;
    private final DishSearchRepository dishSearchRepository;

    public ElasticsearchSearchService(
            @org.springframework.beans.factory.annotation.Autowired(required = false)
            ElasticsearchOperations elasticsearchOperations,
            @org.springframework.beans.factory.annotation.Autowired(required = false)
            DishSearchRepository dishSearchRepository) {
        this.elasticsearchOperations = elasticsearchOperations;
        this.dishSearchRepository = dishSearchRepository;
    }

    public List<Dish> search(String keywords, int limit) {
        if (keywords == null || keywords.trim().isEmpty()) {
            return Collections.emptyList();
        }

        try {
            if (elasticsearchOperations == null || dishSearchRepository == null) {
                return Collections.emptyList();
            }

            Criteria criteria = new Criteria("name").contains(keywords)
                    .or(new Criteria("spicyLevel").contains(keywords));

            CriteriaQuery query = new CriteriaQuery(criteria);
            query.setMaxResults(limit);

            SearchHits<DishDocument> hits = elasticsearchOperations.search(query, DishDocument.class);

            return hits.getSearchHits().stream()
                    .map(SearchHit::getContent)
                    .map(this::convertToDish)
                    .toList();

        } catch (Exception e) {
            log.error("Elasticsearch搜索失败: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    public boolean isAvailable() {
        try {
            return elasticsearchOperations != null
                    && elasticsearchOperations.indexOps(DishDocument.class).exists();
        } catch (Exception e) {
            return false;
        }
    }

    public String getEngineName() {
        return "Elasticsearch搜索";
    }

    private Dish convertToDish(DishDocument doc) {
        Dish dish = new Dish();
        dish.setId(Long.valueOf(doc.getId()));
        dish.setName(doc.getName());
        if (doc.getSpicyLevel() != null) {
            dish.setSpicy(Dish.SpicyLevel.valueOf(doc.getSpicyLevel()));
        }
        dish.setIsSignature(doc.getIsSignature());
        dish.setIsDeleted(false);
        return dish;
    }
}
