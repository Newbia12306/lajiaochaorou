package com.example.demo.DTO;

import com.example.demo.model.Dish;
import java.util.List;

public class RecommendationResponse {
    private Boolean success;
    private String query;
    private Integer total;
    private List<DishRecommendation> recommendations;
    private String message;

    public static class DishRecommendation {
        private Dish dish;
        private Double score;
        private String matchReason;
        private String relevance;

        public Dish getDish() { return dish; }
        public void setDish(Dish dish) { this.dish = dish; }

        public Double getScore() { return score; }
        public void setScore(Double score) { this.score = score; }

        public String getMatchReason() { return matchReason; }
        public void setMatchReason(String matchReason) { this.matchReason = matchReason; }

        public String getRelevance() { return relevance; }
        public void setRelevance(String relevance) { this.relevance = relevance; }
    }

    public Boolean getSuccess() { return success; }
    public void setSuccess(Boolean success) { this.success = success; }

    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }

    public Integer getTotal() { return total; }
    public void setTotal(Integer total) { this.total = total; }

    public List<DishRecommendation> getRecommendations() { return recommendations; }
    public void setRecommendations(List<DishRecommendation> recommendations) { this.recommendations = recommendations; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}