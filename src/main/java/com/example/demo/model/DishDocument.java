package com.example.demo.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Document(indexName = "dishes")
public class DishDocument {

    @Id
    @Field(type = FieldType.Keyword)
    private String id;

    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String name;

    @Field(type = FieldType.Keyword)
    private String spicyLevel;

    @Field(type = FieldType.Boolean)
    private Boolean isSignature;

    public DishDocument() {}

    public DishDocument(Dish dish) {
        this.id = String.valueOf(dish.getId());
        this.name = dish.getName();
        this.spicyLevel = dish.getSpicy() != null ? dish.getSpicy().name() : null;
        this.isSignature = dish.getIsSignature();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSpicyLevel() { return spicyLevel; }
    public void setSpicyLevel(String spicyLevel) { this.spicyLevel = spicyLevel; }

    public Boolean getIsSignature() { return isSignature; }
    public void setIsSignature(Boolean isSignature) { this.isSignature = isSignature; }
}
