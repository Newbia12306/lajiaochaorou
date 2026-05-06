package com.example.demo.algorithm;

import java.util.*;

public class InvertedIndex {
    private Map<String, List<Posting>> index;
    private Map<Integer, Integer> docLengths;
    private int docCount;
    private double avgdl;

    public InvertedIndex() {
        this.index = new HashMap<>();
        this.docLengths = new HashMap<>();
        this.docCount = 0;
        this.avgdl = 0.0;
    }

    public void buildIndex(List<List<String>> tokenizedDocs) {
        this.docCount = tokenizedDocs.size();
        Map<String, Map<Integer, Integer>> termFreqMap = new HashMap<>();

        for (int docId = 0; docId < tokenizedDocs.size(); docId++) {
            List<String> tokens = tokenizedDocs.get(docId);
            int docLength = tokens.size();
            docLengths.put(docId, docLength);

            for (String term : tokens) {
                termFreqMap.computeIfAbsent(term, k -> new HashMap<>())
                        .merge(docId, 1, Integer::sum);
            }
        }

        for (Map.Entry<String, Map<Integer, Integer>> entry : termFreqMap.entrySet()) {
            String term = entry.getKey();
            Map<Integer, Integer> docFreqs = entry.getValue();

            List<Posting> postings = new ArrayList<>();
            for (Map.Entry<Integer, Integer> docEntry : docFreqs.entrySet()) {
                int docId = docEntry.getKey();
                int freq = docEntry.getValue();
                double normalizedTf = (double) freq / docLengths.get(docId);
                postings.add(new Posting(docId, normalizedTf));
            }

            index.put(term, postings);
        }

        this.avgdl = docLengths.values().stream()
                .mapToInt(Integer::intValue)
                .average()
                .orElse(0.0);
    }

    public List<Posting> getPostings(String term) {
        return index.getOrDefault(term, null);
    }

    public int getDocCount() {
        return docCount;
    }

    public int getDocLength(int docId) {
        return docLengths.getOrDefault(docId, 0);
    }

    public double getAvgdl() {
        return avgdl;
    }

    public Set<String> getAllTerms() {
        return index.keySet();
    }
}