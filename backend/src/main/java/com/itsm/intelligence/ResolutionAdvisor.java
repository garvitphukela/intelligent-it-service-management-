package com.itsm.intelligence;

import com.itsm.domain.incident.Incident;
import com.itsm.domain.incident.IncidentStatus;
import com.itsm.repository.IncidentRepository;
import org.springframework.stereotype.Component;

import java.util.*;

/** Suggests resolutions used for similar incidents in the past (knowledge re-use). */
@Component
public class ResolutionAdvisor {

    public record SuggestedResolution(Incident source, double similarity, String summary, String rootCause, String steps) {}

    private static final double MIN_SIMILARITY = 0.15;

    private final IncidentRepository incidents;

    public ResolutionAdvisor(IncidentRepository incidents) {
        this.incidents = incidents;
    }

    public List<SuggestedResolution> suggest(String text, Long categoryId, Long serviceId, Long excludeId) {
        List<Incident> history = incidents.findResolvedWithResolution(
                EnumSet.of(IncidentStatus.RESOLVED, IncidentStatus.VERIFIED, IncidentStatus.CLOSED)).stream()
                .filter(i -> excludeId == null || !i.getId().equals(excludeId))
                .toList();
        if (history.isEmpty() || text == null || text.isBlank()) return List.of();

        List<String> corpus = new ArrayList<>(history.stream().map(Incident::searchableText).toList());
        corpus.add(text);
        TfIdfModel model = new TfIdfModel(corpus);
        Map<String, Double> q = model.vectorize(text);

        Map<String, SuggestedResolution> bestPerSummary = new LinkedHashMap<>();
        for (Incident i : history) {
            double sim = TfIdfModel.cosine(q, model.vectorize(i.searchableText()));
            if (categoryId != null && i.getCategory() != null && i.getCategory().getId().equals(categoryId)) sim += 0.1;
            if (serviceId != null && i.getService().getId().equals(serviceId)) sim += 0.05;
            if (sim < MIN_SIMILARITY) continue;
            var r = i.getResolution();
            var s = new SuggestedResolution(i, Math.min(1.0, Math.round(sim * 100) / 100.0), r.getSummary(), r.getRootCause(), r.getSteps());
            bestPerSummary.merge(r.getSummary().toLowerCase(Locale.ROOT), s, (a, b) -> a.similarity() >= b.similarity() ? a : b);
        }
        return bestPerSummary.values().stream()
                .sorted(Comparator.comparingDouble(SuggestedResolution::similarity).reversed())
                .limit(3).toList();
    }
}
