package com.example.demo.service;

import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

@Service
public class LlmService {

    @Value("${llm.api.base-url:https://api.deepseek.com/v1}")
    private String apiBaseUrl;

    @Value("${llm.api.key:}")
    private String apiKey;

    @Value("${llm.chat-model:deepseek-chat}")
    private String chatModel;

    @Value("${llm.use-local:true}")
    private boolean useLocal;

    private ChatLanguageModel chatModelInstance;
    private EmbeddingModel embeddingModelInstance;

    @PostConstruct
    public void init() {
        if (useLocal) {
            // 使用本地模型（免费，适合测试）
            this.embeddingModelInstance = new AllMiniLmL6V2EmbeddingModel();
            System.out.println("使用本地 Embedding 模型");
        } else {
            // 使用云端 API（需要 API Key）
            this.chatModelInstance = OpenAiChatModel.builder()
                    .baseUrl(apiBaseUrl)
                    .apiKey(apiKey)
                    .modelName(chatModel)
                    .temperature(0.7)
                    .build();
            
            this.embeddingModelInstance = OpenAiEmbeddingModel.builder()
                    .baseUrl(apiBaseUrl)
                    .apiKey(apiKey)
                    .modelName("text-embedding-ada-002")
                    .build();
            
            System.out.println("使用云端 LLM API: " + apiBaseUrl);
        }
    }

    public String chat(String userMessage, String context) {
        if (chatModelInstance == null) {
            return "LLM 服务未配置，请设置 API Key 或启用本地模式";
        }

        String fullPrompt = context != null && !context.isEmpty() 
                ? "基于以下菜品信息回答问题：\n" + context + "\n\n用户问题：" + userMessage
                : userMessage;

        try {
            return chatModelInstance.generate(fullPrompt);
        } catch (Exception e) {
            System.err.println("LLM 调用失败: " + e.getMessage());
            return "抱歉，暂时无法处理您的请求";
        }
    }

    public float[] generateEmbedding(String text) {
        try {
            dev.langchain4j.data.embedding.Embedding embedding = 
                    embeddingModelInstance.embed(text).content();
            return embedding.vectorAsList().stream()
                    .mapToFloat(Float::floatValue)
                    .toArray();
        } catch (Exception e) {
            System.err.println("生成嵌入失败: " + e.getMessage());
            return new float[384];
        }
    }

    public String recommendDishes(String tastePreference, String dietaryRestrictions) {
        String prompt = String.format(
            "用户口味偏好：%s\n饮食限制：%s\n\n请推荐适合的菜品类型，并说明推荐理由。",
            tastePreference != null ? tastePreference : "无特殊偏好",
            dietaryRestrictions != null ? dietaryRestrictions : "无限制"
        );

        return chat(prompt, null);
    }
}
