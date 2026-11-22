package com.itsm.intelligence;

import java.util.*;

/**
 * Lightweight NLP pipeline (no external libraries): lower-case, tokenise, drop stop
 * words, map synonyms to a canonical term and apply a tiny suffix stemmer.
 * Deterministic and fast enough to run on every keystroke.
 */
public final class TextAnalyzer {

    private static final Set<String> STOP_WORDS = Set.of(
            "a", "an", "the", "is", "are", "was", "were", "be", "been", "am", "to", "of", "and", "or", "in", "on", "at",
            "for", "with", "from", "by", "it", "its", "this", "that", "these", "those", "i", "we", "you", "they", "he",
            "she", "my", "our", "your", "their", "me", "us", "them", "not", "no", "can", "cannot", "can't", "cant",
            "could", "would", "should", "will", "do", "does", "did", "have", "has", "had", "but", "so", "if", "then",
            "when", "while", "after", "before", "since", "as", "all", "any", "some", "very", "too", "also", "just",
            "please", "help", "hi", "hello", "thanks", "issue", "problem", "getting", "get", "got", "again", "now",
            "today", "still", "being", "unable", "able", "anyone", "everyone", "users", "user", "there", "here");

    /** Synonym groups -> canonical token. Makes "unavailable", "outage" and "down" comparable. */
    private static final Map<String, String> SYNONYMS = new HashMap<>();

    static {
        group("down", "unavailable", "outage", "offline", "unreachable", "dead", "inaccessible", "unresponsive");
        group("fail", "failing", "failed", "failure", "fails", "error", "errors", "broken", "crash", "crashed", "crashing");
        group("slow", "sluggish", "lag", "lagging", "latency", "timeout", "timeouts", "hang", "hanging", "freeze", "frozen");
        group("pay", "payment", "payments", "transaction", "transactions", "checkout", "billing", "invoice");
        group("login", "log", "logon", "signin", "sign", "authenticate", "authentication", "sso", "credential", "credentials", "password");
        group("db", "database", "databases", "sql", "query", "queries", "postgres", "mysql", "oracle");
        group("net", "network", "internet", "wifi", "wi-fi", "lan", "connectivity", "connection", "router", "switch", "dns");
        group("mail", "email", "e-mail", "outlook", "inbox", "smtp");
        group("vpn", "remote", "tunnel");
        group("laptop", "computer", "pc", "desktop", "machine", "notebook");
        group("storage", "disk", "drive", "share", "sharepoint", "onedrive", "s3", "bucket", "quota");
        group("hack", "phishing", "malware", "virus", "breach", "suspicious", "ransomware", "compromised");
    }

    private static void group(String canonical, String... words) {
        SYNONYMS.put(canonical, canonical);
        for (String w : words) SYNONYMS.put(w, canonical);
    }

    private TextAnalyzer() {}

    public static List<String> tokens(String text) {
        if (text == null || text.isBlank()) return List.of();
        List<String> out = new ArrayList<>();
        for (String raw : text.toLowerCase(Locale.ROOT).split("[^a-z0-9\\-']+")) {
            String w = raw.replace("'", "");
            if (w.length() < 2 || STOP_WORDS.contains(w)) continue;
            String canonical = SYNONYMS.get(w);
            if (canonical == null) {
                String stem = stem(w);
                canonical = SYNONYMS.getOrDefault(stem, stem);
            }
            if (!STOP_WORDS.contains(canonical)) out.add(canonical);
        }
        return out;
    }

    /** Very small Porter-like suffix stripper. */
    static String stem(String w) {
        if (w.length() > 5 && w.endsWith("ing")) return w.substring(0, w.length() - 3);
        if (w.length() > 4 && w.endsWith("ed")) return w.substring(0, w.length() - 2);
        if (w.length() > 4 && w.endsWith("ies")) return w.substring(0, w.length() - 3) + "y";
        if (w.length() > 3 && w.endsWith("s") && !w.endsWith("ss")) return w.substring(0, w.length() - 1);
        return w;
    }

    public static Map<String, Integer> termFrequencies(String text) {
        Map<String, Integer> tf = new HashMap<>();
        for (String t : tokens(text)) tf.merge(t, 1, Integer::sum);
        return tf;
    }
}
