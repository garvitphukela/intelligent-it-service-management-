package com.itsm.service;

import com.itsm.domain.incident.Incident;
import com.itsm.domain.incident.IncidentStatus;
import com.itsm.domain.incident.SlaStatus;
import com.itsm.event.Events;
import com.itsm.event.IncidentEventPublisher;
import com.itsm.intelligence.IncidentCorrelator;
import com.itsm.intelligence.SlaRiskPredictor;
import com.itsm.repository.IncidentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * One monitoring cycle: detect SLA breaches, raise early warnings (threshold or predicted),
 * escalate unacknowledged critical incidents and detect correlated major incidents.
 */
@Service
public class SlaMonitoringService {

    public record CycleResult(int checked, int breaches, int warnings, int escalations, int majorIncidents) {}

    private final IncidentRepository incidents;
    private final IncidentEventPublisher events;
    private final EscalationService escalation;
    private final SlaRiskPredictor predictor;
    private final IncidentCorrelator correlator;
    private final SlaService sla;
    private final Clock clock;

    public SlaMonitoringService(IncidentRepository incidents, IncidentEventPublisher events, EscalationService escalation,
                                SlaRiskPredictor predictor, IncidentCorrelator correlator, SlaService sla, Clock clock) {
        this.incidents = incidents;
        this.events = events;
        this.escalation = escalation;
        this.predictor = predictor;
        this.correlator = correlator;
        this.sla = sla;
        this.clock = clock;
    }

    @Transactional
    public CycleResult runCycle() {
        Instant now = Instant.now(clock);
        var averages = predictor.historicalAverages();
        List<Incident> open = incidents.findByStatusIn(IncidentStatus.openStatuses());
        int breaches = 0, warnings = 0, escalations = 0;

        for (Incident i : open) {
            var tracker = i.getSla();
            if (!tracker.isStarted()) continue;
            if (tracker.detectBreaches(now)) {
                breaches++;
                events.publish(new Events.SlaBreached(i, now));
            } else if (tracker.shouldWarn(now)) {
                tracker.markWarningRaised();
                warnings++;
                events.publish(new Events.SlaWarning(i, now, String.format(Locale.ROOT, "%.0f%% of the %s window used",
                        tracker.elapsedFraction(now) * 100, tracker.getRespondedAt() == null ? "response" : "resolution")));
            }
            if (escalation.check(i, now)) escalations++;
            i.refreshSlaStatus(now);

            if (i.getSlaStatus() == SlaStatus.ON_TRACK) {
                var risk = predictor.assess(i, now, averages, sla.timeScale());
                if (risk.isPresent() && risk.get().riskScore() >= 0.8 && risk.get().predictedResolution().isAfter(tracker.getResolutionDueAt())) {
                    i.forceSlaStatus(SlaStatus.AT_RISK);
                    if (!tracker.isWarningRaised()) {
                        tracker.markWarningRaised();
                        warnings++;
                        events.publish(new Events.SlaWarning(i, now, "predicted breach - " + risk.get().reason()));
                    }
                }
            }
        }

        int majors = 0;
        for (var cluster : correlator.findClusters()) {
            boolean fresh = cluster.incidents().stream().anyMatch(i -> !i.isMajorIncident());
            if (!fresh) continue;
            cluster.incidents().forEach(Incident::markMajorIncident);
            Incident representative = cluster.incidents().stream()
                    .filter(i -> i.getService().equals(cluster.rootService()))
                    .findFirst()
                    .orElse(cluster.incidents().stream().min(Comparator.comparing(Incident::getCreatedAt)).orElseThrow());
            events.publish(new Events.MajorIncidentDetected(representative, now,
                    cluster.incidents().stream().map(Incident::getNumber).toList(), cluster.rootService().getName()));
            majors++;
        }
        return new CycleResult(open.size(), breaches, warnings, escalations, majors);
    }
}
