package com.itsm.domain.catalog;

import com.itsm.domain.incident.Priority;
import jakarta.persistence.*;

import java.time.Duration;

/**
 * Service Level Agreement for a priority, optionally specific to one service.
 * A policy with {@code service == null} is the organisation-wide default.
 */
@Entity
@Table(name = "sla_policies", uniqueConstraints = @UniqueConstraint(columnNames = {"service_id", "priority"}))
public class SlaPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 80)
    private String name;

    @ManyToOne
    @JoinColumn(name = "service_id")
    private ITService service;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 5)
    private Priority priority;

    private int responseMinutes;

    private int resolutionMinutes;

    /** Percentage of the window after which an "approaching SLA" warning is raised. */
    private int warningThresholdPercent = 75;

    protected SlaPolicy() {}

    public SlaPolicy(String name, ITService service, Priority priority, int responseMinutes, int resolutionMinutes, int warningThresholdPercent) {
        this.service = service;
        this.priority = priority;
        update(name, responseMinutes, resolutionMinutes, warningThresholdPercent);
    }

    public void update(String name, int responseMinutes, int resolutionMinutes, int warningThresholdPercent) {
        if (responseMinutes <= 0 || resolutionMinutes <= 0) {
            throw new IllegalArgumentException("SLA targets must be positive");
        }
        if (responseMinutes > resolutionMinutes) {
            throw new IllegalArgumentException("Response target cannot exceed resolution target");
        }
        if (warningThresholdPercent < 10 || warningThresholdPercent > 99) {
            throw new IllegalArgumentException("Warning threshold must be between 10 and 99 percent");
        }
        this.name = name;
        this.responseMinutes = responseMinutes;
        this.resolutionMinutes = resolutionMinutes;
        this.warningThresholdPercent = warningThresholdPercent;
    }

    public Duration responseTarget() { return Duration.ofMinutes(responseMinutes); }
    public Duration resolutionTarget() { return Duration.ofMinutes(resolutionMinutes); }

    public Long getId() { return id; }
    public String getName() { return name; }
    public ITService getService() { return service; }
    public Priority getPriority() { return priority; }
    public int getResponseMinutes() { return responseMinutes; }
    public int getResolutionMinutes() { return resolutionMinutes; }
    public int getWarningThresholdPercent() { return warningThresholdPercent; }
}
