package com.example.demo.service;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.List;

@Service
public class EmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingService.class);

    private EmbeddingModel embeddingModel;

    @PostConstruct
    public void init() {
        this.embeddingModel = new AllMiniLmL6V2EmbeddingModel();
        log.info("Embedding 模型初始化完成");
    }

    public float[] generateEmbedding(String text) {
        if (text == null || text.trim().isEmpty()) {
            return new float[384];
        }

        if (embeddingModel == null) {
            log.warn("Embedding model not initialized, returning zero vector");
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
            log.error("生成嵌入失败: {}", e.getMessage(), e);
            return new float[384];
        }
    }

    public List<float[]> generateBatchEmbeddings(List<String> texts) {
        return texts.stream()
                .map(this::generateEmbedding)
                .toList();
    }
}
