package com.example.demo.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.math.BigDecimal;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "menu")

public class Dish{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false,unique = true)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "spicy")
    private SpicyLevel spicy;

    @Column(name = "is_signature")
    private Boolean isSignature = false;

    @Column(name = "is_deleted",nullable = false)
    private Boolean isDeleted = false;

    @Column(columnDefinition = "vector(384)")
    private float[] embedding;

    public enum SpicyLevel{
        NOT_SPICY("不辣"),
        MEDIUM_SPICY("中辣"),
        EXTRA_SPICY("重辣");

        private final String label;

        SpicyLevel(String label){
            this.label = label;
        }
        public String getLabel(){
            return label;
        }
    }



    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public SpicyLevel getSpicy() { return spicy; }
    public void setSpicy(SpicyLevel spicy) { this.spicy = spicy; }

    public Boolean getIsSignature() { return isSignature; }
    public void setIsSignature(Boolean isSignature) { this.isSignature = isSignature; }

    public Boolean getIsDeleted() { return isDeleted; }
    public void setIsDeleted(Boolean idDeleted) { this.isDeleted = idDeleted; }

    public float[] getEmbedding() { return embedding; }
    public void setEmbedding(float[] embedding) { this.embedding = embedding; }


    // 无参构造函数
    public Dish() {}

    // 有参构造函数
    public Dish(String name,SpicyLevel spicy,Boolean isSignature) {
        this.name = name;
        this.spicy = spicy;
        this.isSignature = isSignature;
        this.isDeleted = false;
    }
}

