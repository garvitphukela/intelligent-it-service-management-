package com.itsm.domain.incident;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.time.Duration;
import java.time.Instant;

/**
 * Tracks response/resolution deadlines for one incident. Pure domain logic with no
 * framework dependencies, so it is trivially unit-testable with fixed instants.
 * The resolution clock pauses while the incident waits for the reporter.
 */
@Embeddable
public class SlaTracker {

    @Column(name = "sla_started_at")
    private Instant startedAt;

    @Column(name = "sla_response_due")
    private Instant responseDueAt;

    @Column(name = "sla_resolution_due")
    private Instant resolutionDueAt;

    @Column(name = "sla_responded_at")
    private Instant respondedAt;

    @Column(name = "sla_resolved_at")
    private Instant resolvedAt;

    @Column(name = "sla_paused_at")
    private Instant pausedAt;

    @Column(name = "sla_paused_seconds")
    private long pausedSeconds;

    @Column(name = "sla_warning_percent")
    private int warningPercent = 75;

    @Column(name = "sla_warning_raised")
    private boolean warningRaised;

    @Column(name = "sla_response_breached")
    private boolean responseBreached;

    @Column(name = "sla_resolution_breached")
    private boolean resolutionBreached;

    public SlaTracker() {}

    /** (Re)start the clock from {@code from} with the given windows. */
    public void start(Instant from, Duration responseWindow, Duration resolutionWindow, int warningPercent) {
        this.startedAt = from;
        this.responseDueAt = from.plus(responseWindow);
        this.resolutionDueAt = from.plus(resolutionWindow).plusSeconds(pausedSeconds);
        this.warningPercent = warningPercent;
        this.warningRaised = false;
    }

    public boolean isStarted() { return startedAt != null; }

    public void markResponded(Instant at) {
        if (respondedAt == null) {
            respondedAt = at;
            if (at.isAfter(responseDueAt)) responseBreached = true;
        }
    }

    public void pause(Instant at) {
        if (pausedAt == null) pausedAt = at;
    }

    public void resume(Instant at) {
        if (pausedAt != null) {
            long secs = Math.max(0, Duration.between(pausedAt, at).getSeconds());
            pausedSeconds += secs;
            resolutionDueAt = resolutionDueAt.plusSeconds(secs);
            pausedAt = null;
        }
    }

    public void markResolved(Instant at) {
        resume(at);
        resolvedAt = at;
        if (at.isAfter(resolutionDueAt)) resolutionBreached = true;
    }

    /** Re-opened incidents get their resolution clock back. */
    public void clearResolution() {
        resolvedAt = null;
    }

    /**
     * Evaluate the SLA at {@code now}. Side effect: latches breach flags so a breach is
     * never "forgotten" even if later actions happen.
     * @return true if a <em>new</em> breach was detected by this call
     */
    public boolean detectBreaches(Instant now) {
        if (!isStarted()) return false;
        boolean newBreach = false;
        if (respondedAt == null && !responseBreached && now.isAfter(responseDueAt)) {
            responseBreached = true;
            newBreach = true;
        }
        if (resolvedAt == null && pausedAt == null && !resolutionBreached && now.isAfter(resolutionDueAt)) {
            resolutionBreached = true;
            newBreach = true;
        }
        return newBreach;
    }

    /** Fraction (0..1+) of the currently relevant window that has elapsed. */
    public double elapsedFraction(Instant now) {
        if (!isStarted()) return 0;
        Instant effectiveNow = pausedAt != null ? pausedAt : now;
        if (respondedAt == null) {
            return fraction(startedAt, responseDueAt, effectiveNow);
        }
        long total = Duration.between(startedAt, resolutionDueAt).getSeconds() - pausedSeconds;
        long used = Duration.between(startedAt, effectiveNow).getSeconds() - pausedSeconds;
        return total <= 0 ? 1 : (double) used / total;
    }

    private static double fraction(Instant start, Instant due, Instant now) {
        long total = Duration.between(start, due).getSeconds();
        long used = Duration.between(start, now).getSeconds();
        return total <= 0 ? 1 : (double) used / total;
    }

    public boolean shouldWarn(Instant now) {
        return isStarted() && !warningRaised && !isBreached() && resolvedAt == null
                && elapsedFraction(now) * 100 >= warningPercent;
    }

    public void markWarningRaised() { this.warningRaised = true; }

    public SlaStatus statusAt(Instant now, IncidentStatus incidentStatus) {
        if (!isStarted() || incidentStatus == IncidentStatus.CANCELLED) return SlaStatus.NOT_APPLICABLE;
        if (isBreached()) return SlaStatus.BREACHED;
        if (resolvedAt != null) return SlaStatus.MET;
        if (pausedAt != null) return SlaStatus.PAUSED;
        if (elapsedFraction(now) * 100 >= warningPercent) return SlaStatus.AT_RISK;
        return SlaStatus.ON_TRACK;
    }

    public boolean isBreached() { return responseBreached || resolutionBreached; }

    public Duration responseTime() {
        return respondedAt == null || startedAt == null ? null : Duration.between(startedAt, respondedAt);
    }

    public Duration resolutionTime() {
        return resolvedAt == null || startedAt == null ? null
                : Duration.between(startedAt, resolvedAt).minusSeconds(pausedSeconds);
    }

    public Instant getStartedAt() { return startedAt; }
    public Instant getResponseDueAt() { return responseDueAt; }
    public Instant getResolutionDueAt() { return resolutionDueAt; }
    public Instant getRespondedAt() { return respondedAt; }
    public Instant getResolvedAt() { return resolvedAt; }
    public Instant getPausedAt() { return pausedAt; }
    public long getPausedSeconds() { return pausedSeconds; }
    public int getWarningPercent() { return warningPercent; }
    public boolean isWarningRaised() { return warningRaised; }
    public boolean isResponseBreached() { return responseBreached; }
    public boolean isResolutionBreached() { return resolutionBreached; }
}
