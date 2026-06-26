package com.example.demo.service;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.List;

@Service
public class EmbeddingService {

    private EmbeddingModel embeddingModel;

    @PostConstruct
    public void init() {
        this.embeddingModel = new AllMiniLmL6V2EmbeddingModel();
        System.out.println("Embedding 模型初始化完成");
    }

    public float[] generateEmbedding(String text) {
        if (text == null || text.trim().isEmpty()) {
            return new float[384];
        }
        
        try {
            Embedding embedding = embeddingModel.embed(text).content();
            List<Float> vectorList = embedding.vectorAsList();
            float[] result = new float[vectorList.size()];
            for (int i = 0; i < vectorList.size(); i++) {
                result[i] = vectorList.get(i);
            }
            return result;
        } catch (Exception e) {
            System.err.println("生成嵌入失败: " + e.getMessage());
            return new float[384];
        }
    }

    public List<float[]> generateBatchEmbeddings(List<String> texts) {
        return texts.stream()
                .map(this::generateEmbedding)
                .toList();
    }
}
