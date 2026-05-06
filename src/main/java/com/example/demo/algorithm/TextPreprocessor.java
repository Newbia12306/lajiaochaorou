package com.example.demo.algorithm;

import java.util.*;
import java.util.stream.Collectors;

public class TextPreprocessor {
    private Map<String, List<String>> synonymMap;

    public TextPreprocessor() {
        this.synonymMap = new HashMap<>();
        initializeDefaultSynonyms();
    }

    private void initializeDefaultSynonyms() {
        synonymMap.put("宫爆", Arrays.asList("宫保"));
        synonymMap.put("番茄", Arrays.asList("西红柿"));
        synonymMap.put("马铃薯", Arrays.asList("土豆", "洋芋"));
        synonymMap.put("微辣", Arrays.asList("不太辣", "一点点辣"));
        synonymMap.put("中辣", Arrays.asList("一般辣", "中等辣"));
        synonymMap.put("重辣", Arrays.asList("很辣", "非常辣", "特辣"));
        synonymMap.put("不辣", Arrays.asList("清淡", "微甜"));
    }

    public List<String> tokenize(String text) {
        if (text == null || text.trim().isEmpty()) {
            return Collections.emptyList();
        }

        List<String> tokens = new ArrayList<>();
        StringBuilder currentWord = new StringBuilder();
        String lowerText = text.toLowerCase();

        for (char c : lowerText.toCharArray()) {
            // 1. 如果是中文汉字，强制拆分为单字
            if (c >= '\u4e00' && c <= '\u9fa5') {
                if (currentWord.length() > 0) {
                    tokens.add(currentWord.toString());
                    currentWord = new StringBuilder();
                }
                tokens.add(String.valueOf(c));
            }
            // 2. 如果是空格或标点，作为分隔符
            else if (Character.isWhitespace(c) || "，。.!！?？、".indexOf(c) != -1) {
                if (currentWord.length() > 0) {
                    tokens.add(currentWord.toString());
                    currentWord = new StringBuilder();
                }
            }
            // 3. 其他字符（英文/数字）合并
            else {
                currentWord.append(c);
            }
        }
        
        if (currentWord.length() > 0) {
            tokens.add(currentWord.toString());
        }

        // 打印日志方便调试
        System.out.println("  分词 ['" + text + "'] -> " + tokens);
        return tokens;
    }

    public List<String> expandQuery(List<String> queryTokens) {
        Set<String> expandedTokens = new LinkedHashSet<>(queryTokens);

        for (String token : queryTokens) {
            List<String> synonyms = synonymMap.get(token);
            if (synonyms != null) {
                expandedTokens.addAll(synonyms);
            }
        }

        return new ArrayList<>(expandedTokens);
    }

    public String preprocessDish(String name, String spicy, boolean isSignature) {
        StringBuilder sb = new StringBuilder();
        sb.append(name).append(" ");
        sb.append(spicy).append(" ");

        if (isSignature) {
            sb.append("招牌菜 ");
        }

        return sb.toString().trim();
    }

    public void loadSynonymsFromMap(Map<String, List<String>> customSynonyms) {
        if (customSynonyms != null) {
            synonymMap.putAll(customSynonyms);
        }
    }
}