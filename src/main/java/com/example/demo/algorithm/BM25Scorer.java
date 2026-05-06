package com.example.demo.algorithm;

import java.util.*;
import java.util.stream.Collectors;

public class BM25Scorer {
    private static final double K1 = 1.5;
    private static final double B = 0.75;

    private final InvertedIndex index;

    public BM25Scorer(InvertedIndex index) {
        this.index = index;
    }

    public double[] score(List<String> queryTokens) {
        int docCount = index.getDocCount();
        double[] scores = new double[docCount];

        for (String term : queryTokens) {
            List<Posting> postings = index.getPostings(term);

            if (postings == null || postings.isEmpty()) {
                continue;
            }

            double idf = calculateIDF(postings.size(), docCount);

            for (Posting p : postings) {
                int docId = p.getDocId();
                double tf = p.getTermFreq();
                int docLength = index.getDocLength(docId);
                double docLenNorm = docLength / index.getAvgdl();

                double numerator = tf * (K1 + 1);
                double denominator = tf + K1 * (1 - B + B * docLenNorm);
                double bm25Score = (numerator / denominator) * idf;

                scores[docId] += bm25Score;
            }
        }

        return scores;
    }

    private double calculateIDF(int postingCount, int totalDocs) {
        return Math.log((totalDocs - postingCount + 0.5) / (postingCount + 0.5) + 1.0);
    }

    public List<Integer> getTopKIndices(double[] scores, int topK) {
        Map<Integer, Double> scoreMap = new HashMap<>();
        for (int i = 0; i < scores.length; i++) {
            if (scores[i] > 0) {
                scoreMap.put(i, scores[i]);
            }
        }

        return scoreMap.entrySet().stream()
                .sorted(Map.Entry.<Integer, Double>comparingByValue().reversed())
                .limit(topK)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }
}