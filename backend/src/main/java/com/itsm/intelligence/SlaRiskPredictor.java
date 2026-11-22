package com.itsm.intelligence;

import com.itsm.domain.incident.Incident;
import com.itsm.domain.incident.IncidentStatus;
import com.itsm.repository.IncidentRepository;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * Predicts SLA breaches before they happen, using the historical average resolution
 * time for the incident's category and priority. An incident is "at risk" when
 * (time already used + expected remaining work) exceeds its resolution deadline.
 */
@Component
public class SlaRiskPredictor {

    public record Risk(Incident incident, double riskScore, Instant predictedResolution, String reason) {}

    private final IncidentRepository incidents;

    public SlaRiskPredictor(IncidentRepository incidents) {
        this.incidents = incidents;
    }

    /** Average historical resolution duration keyed by "category|priority". */
    public Map<String, Duration> historicalAverages() {
        Map<String, List<Duration>> buckets = new HashMap<>();
        for (Incident i : incidents.findResolvedWithResolution(EnumSet.of(IncidentStatus.RESOLVED, IncidentStatus.VERIFIED, IncidentStatus.CLOSED))) {
            Duration d = i.getSla().resolutionTime();
            if (d == null || i.getCategory() == null || i.getPriority() == null) continue;
            buckets.computeIfAbsent(key(i), k -> new ArrayList<>()).add(d);
        }
        Map<String, Duration> avg = new HashMap<>();
        buckets.forEach((k, list) -> avg.put(k, Duration.ofSeconds((long) list.stream().mapToLong(Duration::getSeconds).average().orElse(0))));
        return avg;
    }

    public Optional<Risk> assess(Incident i, Instant now, Map<String, Duration> averages, double timeScale) {
        var sla = i.getSla();
        if (!sla.isStarted() || !i.getStatus().isOpen() || sla.isBreached() || sla.getPausedAt() != null) return Optional.empty();
        double elapsed = sla.elapsedFraction(now);
        Duration window = Duration.between(sla.getStartedAt(), sla.getResolutionDueAt()).minusSeconds(sla.getPausedSeconds());
        Duration hist = averages.get(key(i));
        // Historical durations are in "policy time"; scale them to wall-clock time like the SLA windows.
        Duration expected = hist == null ? window.multipliedBy(6).dividedBy(10)
                : Duration.ofSeconds((long) (hist.getSeconds() / Math.max(1e-9, timeScale)));

        Instant workStart = sla.getRespondedAt() != null ? sla.getRespondedAt() : now;
        Instant predicted = workStart.plus(expected);
        if (predicted.isBefore(now)) predicted = now.plus(expected.dividedBy(4));
        long windowSecs = Math.max(1, window.getSeconds());
        double predictedFraction = (double) Duration.between(sla.getStartedAt(), predicted).getSeconds() / windowSecs;
        double risk = Math.min(1.0, Math.max(elapsed, predictedFraction * 0.8));
        if (i.getAssignedTechnician() == null) risk = Math.min(1.0, risk + 0.15);

        String reason;
        if (predicted.isAfter(sla.getResolutionDueAt())) {
            reason = "Similar " + i.getCategory().getName() + " " + i.getPriority() + " incidents take ~"
                    + humanize(hist == null ? expected : hist) + "; predicted to finish after the deadline";
            risk = Math.max(risk, 0.8);
        } else if (elapsed * 100 >= sla.getWarningPercent()) {
            reason = String.format(Locale.ROOT, "%.0f%% of the SLA window already used", elapsed * 100);
        } else if (i.getAssignedTechnician() == null) {
            reason = "No technician assigned yet";
        } else {
            reason = String.format(Locale.ROOT, "Expected to use ~%.0f%% of the window based on similar past incidents (%.0f%% used so far)",
                    Math.min(99, predictedFraction * 100), elapsed * 100);
        }
        return Optional.of(new Risk(i, Math.round(risk * 100) / 100.0, predicted, reason));
    }

    private static String key(Incident i) { return i.getCategory().getId() + "|" + i.getPriority(); }

    static String humanize(Duration d) {
        long m = d.toMinutes();
        if (m < 60) return m + " min";
        if (m < 60 * 24) return String.format(Locale.ROOT, "%.1f h", m / 60.0);
        return String.format(Locale.ROOT, "%.1f d", m / 1440.0);
    }
}
