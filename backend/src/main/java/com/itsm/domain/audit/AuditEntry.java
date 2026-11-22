package com.itsm.domain.audit;

import jakarta.persistence.*;

import java.time.Instant;

/** Immutable audit record. No setters: once written, history cannot be altered. */
@Entity
@Table(name = "audit_entries", indexes = {@Index(columnList = "incidentId"), @Index(columnList = "timestamp")})
public class AuditEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Instant timestamp;

    private Long actorId;

    @Column(nullable = false, length = 140)
    private String actorName;

    @Column(nullable = false, length = 40)
    private String action;

    private Long incidentId;

    @Column(length = 20)
    private String incidentNumber;

    @Column(length = 20)
    private String previousState;

    @Column(length = 20)
    private String newState;

    @Column(length = 1000)
    private String description;

    protected AuditEntry() {}

    public AuditEntry(Instant timestamp, Long actorId, String actorName, String action, Long incidentId,
                      String incidentNumber, String previousState, String newState, String description) {
        this.timestamp = timestamp;
        this.actorId = actorId;
        this.actorName = actorName;
        this.action = action;
        this.incidentId = incidentId;
        this.incidentNumber = incidentNumber;
        this.previousState = previousState;
        this.newState = newState;
        this.description = description;
    }

    public Long getId() { return id; }
    public Instant getTimestamp() { return timestamp; }
    public Long getActorId() { return actorId; }
    public String getActorName() { return actorName; }
    public String getAction() { return action; }
    public Long getIncidentId() { return incidentId; }
    public String getIncidentNumber() { return incidentNumber; }
    public String getPreviousState() { return previousState; }
    public String getNewState() { return newState; }
    public String getDescription() { return description; }
}
