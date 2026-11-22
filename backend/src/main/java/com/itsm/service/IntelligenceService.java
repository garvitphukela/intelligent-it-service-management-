package com.itsm.service;

import com.itsm.domain.catalog.ITService;
import com.itsm.domain.incident.Impact;
import com.itsm.domain.incident.IncidentStatus;
import com.itsm.domain.incident.Urgency;
import com.itsm.intelligence.*;
import com.itsm.priority.PriorityInput;
import com.itsm.priority.PriorityResult;
import com.itsm.repository.CategoryRepository;
import com.itsm.repository.ITServiceRepository;
import com.itsm.repository.IncidentRepository;
import com.itsm.web.dto.Mapper;
import com.itsm.web.dto.Requests;
import com.itsm.web.dto.Views.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;

/** Facade over the intelligent features, used by the UI while an incident is being typed and by dashboards. */
@Service
@Transactional(readOnly = true)
public class IntelligenceService {

    private final AutoCategorizer categorizer;
    private final DuplicateDetector duplicates;
    private final ResolutionAdvisor resolutions;
    private final IncidentCorrelator correlator;
    private final SlaRiskPredictor riskPredictor;
    private final PriorityService priority;
    private final SlaService sla;
    private final ITServiceRepository services;
    private final CategoryRepository categories;
    private final IncidentRepository incidents;
    private final Clock clock;

    public IntelligenceService(AutoCategorizer categorizer, DuplicateDetector duplicates, ResolutionAdvisor resolutions,
                               IncidentCorrelator correlator, SlaRiskPredictor riskPredictor, PriorityService priority,
                               SlaService sla, ITServiceRepository services, CategoryRepository categories,
                               IncidentRepository incidents, Clock clock) {
        this.categorizer = categorizer;
        this.duplicates = duplicates;
        this.resolutions = resolutions;
        this.correlator = correlator;
        this.riskPredictor = riskPredictor;
        this.priority = priority;
        this.sla = sla;
        this.services = services;
        this.categories = categories;
        this.incidents = incidents;
        this.clock = clock;
    }

    public AnalysisView analyze(Requests.Analyze req) {
        String text = ((req.title() == null ? "" : req.title()) + " " + (req.description() == null ? "" : req.description())).trim();
        ITService service = req.serviceId() == null ? null : services.findById(req.serviceId()).orElse(null);

        var suggestions = categorizer.suggest(text);
        List<CategorySuggestionView> cats = suggestions.stream()
                .map(s -> new CategorySuggestionView(s.category().getId(), s.category().getName(), s.confidence(), s.matchedTerms()))
                .toList();
        Long categoryId = req.categoryId() != null ? req.categoryId() : suggestions.isEmpty() ? null : suggestions.get(0).category().getId();
        String categoryName = categoryId == null ? null : categories.findById(categoryId).map(c -> c.getName()).orElse(null);

        List<DuplicateView> dups = text.length() < 8 ? List.of()
                : duplicates.findSimilar(text, service, categoryId, null).stream().map(Mapper::duplicate).toList();

        List<ResolutionSuggestionView> res = text.length() < 8 ? List.of()
                : resolutions.suggest(text, categoryId, service == null ? null : service.getId(), null).stream()
                .map(r -> new ResolutionSuggestionView(r.source().getId(), r.source().getNumber(), r.source().getTitle(),
                        r.similarity(), r.summary(), r.rootCause(), r.steps()))
                .toList();

        PriorityPreview preview = null;
        if (service != null && req.impact() != null && req.urgency() != null) {
            PriorityResult pr = priority.calculate(new PriorityInput(req.impact(), req.urgency(), service.getBusinessCriticality(),
                    req.affectedUsers() == null ? 1 : req.affectedUsers(), service.getUserBase(), categoryName));
            var policy = sla.findPolicy(service, pr.priority());
            preview = new PriorityPreview(pr.priority().name(), pr.priority().label(), pr.score(), pr.rationale(),
                    priority.activeStrategyName(), policy.map(p -> p.getResponseMinutes()).orElse(null),
                    policy.map(p -> p.getResolutionMinutes()).orElse(null));
        }
        return new AnalysisView(cats, dups, res, preview);
    }

    public List<ResolutionSuggestionView> suggestedResolutions(Long incidentId) {
        var i = incidents.findById(incidentId).orElseThrow(() -> new com.itsm.exception.IncidentNotFoundException("Incident " + incidentId + " not found"));
        return resolutions.suggest(i.searchableText(), i.getCategory() == null ? null : i.getCategory().getId(),
                        i.getService().getId(), i.getId()).stream()
                .map(r -> new ResolutionSuggestionView(r.source().getId(), r.source().getNumber(), r.source().getTitle(),
                        r.similarity(), r.summary(), r.rootCause(), r.steps()))
                .toList();
    }

    public List<DuplicateView> similarTo(Long incidentId) {
        var i = incidents.findById(incidentId).orElseThrow(() -> new com.itsm.exception.IncidentNotFoundException("Incident " + incidentId + " not found"));
        return duplicates.findSimilar(i.searchableText(), i.getService(), i.getCategory() == null ? null : i.getCategory().getId(), i.getId())
                .stream().map(Mapper::duplicate).toList();
    }

    public List<CorrelationView> correlations() {
        return correlator.findClusters().stream()
                .map(c -> new CorrelationView(new Ref(c.rootService().getId(), c.rootService().getName()),
                        c.incidents().stream().map(Mapper::incident).toList(), c.affectedServices(), c.directlyOnRoot(), c.explanation()))
                .toList();
    }

    public List<SlaRiskView> slaRisks() {
        Instant now = Instant.now(clock);
        var averages = riskPredictor.historicalAverages();
        return incidents.findByStatusIn(IncidentStatus.openStatuses()).stream()
                .map(i -> riskPredictor.assess(i, now, averages, sla.timeScale()))
                .flatMap(java.util.Optional::stream)
                .filter(r -> r.riskScore() >= 0.5)
                .sorted(Comparator.comparingDouble(SlaRiskPredictor.Risk::riskScore).reversed())
                .map(r -> new SlaRiskView(Mapper.incident(r.incident()), r.riskScore(), r.predictedResolution(), r.reason()))
                .toList();
    }

    /** Exposed for analysis previews when the caller has only enums. */
    public PriorityResult previewPriority(Impact impact, Urgency urgency, ITService service, int users, String category) {
        return priority.calculate(new PriorityInput(impact, urgency, service.getBusinessCriticality(), users, service.getUserBase(), category));
    }
}
