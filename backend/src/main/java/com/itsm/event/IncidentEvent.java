package com.itsm.event;

import com.itsm.domain.incident.Incident;
import com.itsm.domain.user.User;

import java.time.Instant;
import java.util.Set;

/**
 * Base class of the Observer pattern's subject messages. Each subclass knows how to
 * describe itself and who should hear about it (polymorphism), so listeners stay generic.
 */
public abstract class IncidentEvent {

    private final Incident incident;
    private final User actor;
    private final Instant occurredAt;

    protected IncidentEvent(Incident incident, User actor, Instant occurredAt) {
        this.incident = incident;
        this.actor = actor;
        this.occurredAt = occurredAt;
    }

    /** Stable type name, e.g. INCIDENT_CREATED. Used for audit "action" and notification type. */
    public abstract String type();

    /** Human readable sentence for audit logs and notifications. */
    public abstract String description();

    /** Short notification title. */
    public String title() { return incident.getNumber() + " · " + type().replace('_', ' ').toLowerCase(); }

    public Set<Audience> audience() { return Set.of(); }

    /** Urgent events may additionally go out by SMS. */
    public boolean isUrgent() { return false; }

    public String previousState() { return null; }

    public String newState() { return incident.getStatus().name(); }

    public Incident incident() { return incident; }
    public User actor() { return actor; }
    public Instant occurredAt() { return occurredAt; }

    public String actorName() { return actor == null ? "SYSTEM" : actor.getFullName() + " (" + actor.getCode() + ")"; }
}
