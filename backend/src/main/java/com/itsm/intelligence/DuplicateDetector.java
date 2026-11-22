package com.itsm.intelligence;

import com.itsm.config.ItsmProperties;
import com.itsm.domain.catalog.ITService;
import com.itsm.domain.incident.Incident;
import com.itsm.domain.incident.IncidentStatus;
import com.itsm.repository.IncidentRepository;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * Finds recent incidents that probably describe the same problem.
 * similarity = cosine(TF-IDF) + 0.15 same service + 0.08 dependent service + 0.05 same category (capped at 1).
 */
@Component
public class DuplicateDetector {

    public record Match(Incident incident, double similarity, boolean likelyDuplicate, List<String> reasons) {}

    private final IncidentRepository incidents;
    private final ItsmProperties props;
    private final Clock clock;

    public DuplicateDetector(IncidentRepository incidents, ItsmProperties props, Clock clock) {
        this.incidents = incidents;
        this.props = props;
        this.clock = clock;
    }

    public List<Match> findSimilar(String text, ITService service, Long categoryId, Long excludeId) {
        var cfg = props.getIntelligence();
        Instant since = Instant.now(clock).minus(Duration.ofDays(cfg.getDuplicateWindowDays()));
        List<Incident> pool = incidents.findByCreatedAtAfterAndStatusNot(since, IncidentStatus.CANCELLED).stream()
                .filter(i -> excludeId == null || !i.getId().equals(excludeId))
                .toList();
        if (pool.isEmpty() || text == null || text.isBlank()) return List.of();

        List<String> corpus = new ArrayList<>(pool.stream().map(Incident::searchableText).toList());
        corpus.add(text);
        TfIdfModel model = new TfIdfModel(corpus);
        Map<String, Double> query = model.vectorize(text);

        List<Match> matches = new ArrayList<>();
        for (Incident i : pool) {
            double sim = TfIdfModel.cosine(query, model.vectorize(i.searchableText()));
            List<String> reasons = new ArrayList<>();
            if (sim > 0) reasons.add(String.format(Locale.ROOT, "%.0f%% text overlap", sim * 100));
            if (service != null) {
                if (i.getService().getId().equals(service.getId())) {
                    sim += 0.15;
                    reasons.add("same service");
                } else if (service.transitivelyDependsOn(i.getService()) || i.getService().transitivelyDependsOn(service)) {
                    sim += 0.08;
                    reasons.add("dependent service");
                }
            }
            if (categoryId != null && i.getCategory() != null && i.getCategory().getId().equals(categoryId)) {
                sim += 0.05;
                reasons.add("same category");
            }
            sim = Math.min(1.0, sim);
            if (sim >= cfg.getDuplicateThreshold()) {
                matches.add(new Match(i, Math.round(sim * 100) / 100.0, sim >= cfg.getLikelyDuplicateThreshold(), reasons));
            }
        }
        matches.sort(Comparator.comparingDouble(Match::similarity).reversed());
        return matches.stream().limit(5).toList();
    }
}
