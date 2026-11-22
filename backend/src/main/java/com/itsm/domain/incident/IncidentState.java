package com.itsm.domain.incident;

import com.itsm.domain.user.Technician;

import java.time.Instant;
import java.util.Set;

/**
 * State pattern contract. Each lifecycle state is a separate class that knows which
 * actions are legal and what the next state is. Adding a new state means adding a class
 * and registering it in {@link IncidentStateFactory}; the Incident class does not change.
 */
public interface IncidentState {

    IncidentStatus status();

    Set<IncidentStatus> allowedTransitions();

    Set<IncidentAction> supportedActions();

    default boolean supports(IncidentAction action) { return supportedActions().contains(action); }

    void classify(Incident incident, Instant at);

    void assign(Incident incident, Technician technician, Instant at);

    void acknowledge(Incident incident, Instant at);

    void startWork(Incident incident, Instant at);

    void requestInfo(Incident incident, Instant at);

    void provideInfo(Incident incident, Instant at);

    void resolve(Incident incident, Resolution resolution, Instant at);

    void verify(Incident incident, Instant at);

    void reopen(Incident incident, Instant at);

    void close(Incident incident, String notes, String closedBy, Instant at);

    void cancel(Incident incident, String reason, Instant at);
}
