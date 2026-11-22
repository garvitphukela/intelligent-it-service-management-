package com.itsm.intelligence;

import com.itsm.domain.catalog.Category;
import com.itsm.domain.incident.Incident;
import com.itsm.repository.CategoryRepository;
import com.itsm.repository.IncidentRepository;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Suggests a category from free text by combining two signals:
 * <ol>
 *   <li><b>Rule-based</b>: administrator-maintained keywords per category (weight 2 per hit).</li>
 *   <li><b>Learned</b>: P(category | term) estimated from previously categorised incidents
 *       (a naive-Bayes style vote), so the system improves as history grows.</li>
 * </ol>
 */
@Component
public class AutoCategorizer {

    public record Suggestion(Category category, double score, double confidence, List<String> matchedTerms) {}

    private final CategoryRepository categories;
    private final IncidentRepository incidents;

    public AutoCategorizer(CategoryRepository categories, IncidentRepository incidents) {
        this.categories = categories;
        this.incidents = incidents;
    }

    public List<Suggestion> suggest(String text) {
        List<String> tokens = TextAnalyzer.tokens(text);
        if (tokens.isEmpty()) return List.of();
        String lower = text.toLowerCase(Locale.ROOT);
        List<Category> active = categories.findByActiveTrueOrderByNameAsc();
        Map<String, Map<Long, Integer>> learned = learnedTermCounts();

        Map<Category, Double> scores = new LinkedHashMap<>();
        Map<Category, List<String>> matched = new HashMap<>();
        for (Category c : active) {
            double score = 0;
            List<String> hits = new ArrayList<>();
            for (String kw : c.getKeywords()) {
                boolean hit = kw.contains(" ") ? lower.contains(kw) : tokens.contains(canonical(kw)) || tokens.contains(kw);
                if (hit) {
                    score += 2.0;
                    hits.add(kw);
                }
            }
            for (String t : new HashSet<>(tokens)) {
                Map<Long, Integer> byCat = learned.get(t);
                if (byCat == null) continue;
                int total = byCat.values().stream().mapToInt(Integer::intValue).sum();
                if (total < 2) continue;
                double p = byCat.getOrDefault(c.getId(), 0) / (double) total;
                score += p;
            }
            if (score > 0) {
                scores.put(c, score);
                matched.put(c, hits);
            }
        }
        double sum = scores.values().stream().mapToDouble(Double::doubleValue).sum();
        return scores.entrySet().stream()
                .sorted(Map.Entry.<Category, Double>comparingByValue().reversed())
                .limit(3)
                .map(e -> new Suggestion(e.getKey(), round(e.getValue()), round(confidence(e.getValue(), sum)), matched.get(e.getKey())))
                .toList();
    }

    public Optional<Suggestion> best(String text) {
        return suggest(text).stream().findFirst();
    }

    /** Confidence = share of total score, damped when evidence is thin. */
    private static double confidence(double score, double sum) {
        double share = score / sum;
        double evidence = Math.min(1.0, score / 4.0);
        return Math.min(0.99, share * (0.5 + 0.5 * evidence));
    }

    private static String canonical(String kw) {
        List<String> t = TextAnalyzer.tokens(kw);
        return t.isEmpty() ? kw : t.get(0);
    }

    private Map<String, Map<Long, Integer>> learnedTermCounts() {
        Map<String, Map<Long, Integer>> counts = new HashMap<>();
        for (Incident i : incidents.findAllCategorised()) {
            Long cid = i.getCategory().getId();
            for (String t : new HashSet<>(TextAnalyzer.tokens(i.searchableText()))) {
                counts.computeIfAbsent(t, k -> new HashMap<>()).merge(cid, 1, Integer::sum);
            }
        }
        return counts;
    }

    private static double round(double v) { return Math.round(v * 100) / 100.0; }
}
