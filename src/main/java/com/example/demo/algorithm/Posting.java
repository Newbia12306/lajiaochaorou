package com.example.demo.algorithm;

public class Posting {
    private final int docId;
    private final double termFreq;

    public Posting(int docId, double termFreq) {
        this.docId = docId;
        this.termFreq = termFreq;
    }

    public int getDocId() {
        return docId;
    }

    public double getTermFreq() {
        return termFreq;
    }

    @Override
    public String toString() {
        return String.format("Posting{docId=%d, termFreq=%.4f}", docId, termFreq);
    }
}