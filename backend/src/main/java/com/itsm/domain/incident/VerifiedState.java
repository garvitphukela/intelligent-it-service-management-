package com.itsm.domain.incident;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;

final class VerifiedState extends AbstractIncidentState {
    @Override public IncidentStatus status() { return IncidentStatus.VERIFIED; }
    @Override public Set<IncidentStatus> allowedTransitions() { return EnumSet.of(IncidentStatus.CLOSED); }
    @Override public Set<IncidentAction> supportedActions() { return EnumSet.of(IncidentAction.CLOSE); }

    @Override
    public void close(Incident i, String notes, String closedBy, Instant at) {
        i.recordClosure(notes, closedBy, at);
        i.moveTo(IncidentStatus.CLOSED, at);
    }
}
