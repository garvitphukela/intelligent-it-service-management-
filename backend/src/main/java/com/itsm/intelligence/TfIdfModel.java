package com.itsm.intelligence;

import java.util.*;

/**
 * TF-IDF vector space model over a small corpus, with cosine similarity.
 * Built per request from the relevant incidents (hundreds, not millions), so no index is needed.
 */
public final class TfIdfModel {

    private final Map<String, Integer> documentFrequency = new HashMap<>();
    private final int documentCount;

    public TfIdfModel(Collection<String> corpus) {
        for (String doc : corpus) {
            for (String term : new HashSet<>(TextAnalyzer.tokens(doc))) {
                documentFrequency.merge(term, 1, Integer::sum);
            }
        }
        this.documentCount = Math.max(1, corpus.size());
    }

    public Map<String, Double> vectorize(String text) {
        Map<String, Integer> tf = TextAnalyzer.termFrequencies(text);
        Map<String, Double> v = new HashMap<>();
        tf.forEach((term, count) -> {
            double idf = Math.log((1.0 + documentCount) / (1.0 + documentFrequency.getOrDefault(term, 0))) + 1.0;
            v.put(term, (1 + Math.log(count)) * idf);
        });
        return v;
    }

    public static double cosine(Map<String, Double> a, Map<String, Double> b) {
        if (a.isEmpty() || b.isEmpty()) return 0;
        Map<String, Double> small = a.size() < b.size() ? a : b;
        Map<String, Double> large = small == a ? b : a;
        double dot = 0;
        for (var e : small.entrySet()) {
            Double o = large.get(e.getKey());
            if (o != null) dot += e.getValue() * o;
        }
        return dot / (norm(a) * norm(b));
    }

    private static double norm(Map<String, Double> v) {
        double s = 0;
        for (double x : v.values()) s += x * x;
        return Math.sqrt(s);
    }

    public double similarity(String a, String b) {
        return cosine(vectorize(a), vectorize(b));
    }
}
