package com.example.demo.algorithm;

import java.util.*;
import java.util.stream.Collectors;

public class BM25Algorithm {

    private InvertedIndex invertedIndex;
    private TextPreprocessor textPreprocessor;
    private List<String> documents;

    public BM25Algorithm() {
        this.invertedIndex = new InvertedIndex();
        this.textPreprocessor = new TextPreprocessor();
        this.documents = new ArrayList<>();
    }

    public void buildIndex(List<String> docs) {
        this.documents = new ArrayList<>(docs);

        List<List<String>> tokenizedDocs = docs.stream()
                .map(textPreprocessor::tokenize)
                .collect(Collectors.toList());

        invertedIndex.buildIndex(tokenizedDocs);
    }

    public List<SearchResult> search(String queryText, int topK) {
        List<String> queryTokens = textPreprocessor.tokenize(queryText);

        if (queryTokens.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> expandedTokens = textPreprocessor.expandQuery(queryTokens);

        BM25Scorer scorer = new BM25Scorer(invertedIndex);
        double[] scores = scorer.score(expandedTokens);

        List<Integer> topIndices = scorer.getTopKIndices(scores, topK);

        return topIndices.stream()
                .map(idx -> new SearchResult(idx, scores[idx]))
                .collect(Collectors.toList());
    }

    public void setCustomSynonyms(Map<String, List<String>> synonyms) {
        textPreprocessor.loadSynonymsFromMap(synonyms);
    }

    public static class SearchResult {
        private final int documentId;
        private final double score;

        public SearchResult(int documentId, double score) {
            this.documentId = documentId;
            this.score = score;
        }

        public int getDocumentId() {
            return documentId;
        }

        public double getScore() {
            return score;
        }

        @Override
        public String toString() {
            return String.format("Document %d: %.4f", documentId, score);
        }
    }
}