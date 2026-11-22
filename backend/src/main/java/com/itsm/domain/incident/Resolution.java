package com.itsm.domain.incident;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.time.Instant;

/** Value object describing how an incident was fixed. */
@Embeddable
public class Resolution {

    @Column(name = "resolution_summary", length = 1000)
    private String summary;

    @Column(name = "resolution_root_cause", length = 1000)
    private String rootCause;

    @Column(name = "resolution_steps", length = 4000)
    private String steps;

    @Column(name = "resolved_by", length = 140)
    private String resolvedBy;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    protected Resolution() {}

    public Resolution(String summary, String rootCause, String steps, String resolvedBy, Instant resolvedAt) {
        if (summary == null || summary.isBlank()) {
            throw new IllegalArgumentException("Resolution summary is required");
        }
        this.summary = summary.trim();
        this.rootCause = rootCause;
        this.steps = steps;
        this.resolvedBy = resolvedBy;
        this.resolvedAt = resolvedAt;
    }

    public String getSummary() { return summary; }
    public String getRootCause() { return rootCause; }
    public String getSteps() { return steps; }
    public String getResolvedBy() { return resolvedBy; }
    public Instant getResolvedAt() { return resolvedAt; }

    public String fullText() {
        StringBuilder sb = new StringBuilder(summary == null ? "" : summary);
        if (rootCause != null) sb.append(' ').append(rootCause);
        if (steps != null) sb.append(' ').append(steps);
        return sb.toString();
    }
}
