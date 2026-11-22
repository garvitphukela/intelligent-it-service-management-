package com.itsm.domain.incident;

import com.itsm.domain.user.Technician;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;

final class ClassifiedState extends AbstractIncidentState {
    @Override public IncidentStatus status() { return IncidentStatus.CLASSIFIED; }
    @Override public Set<IncidentStatus> allowedTransitions() { return EnumSet.of(IncidentStatus.ASSIGNED, IncidentStatus.CANCELLED); }
    @Override public Set<IncidentAction> supportedActions() { return EnumSet.of(IncidentAction.ASSIGN, IncidentAction.CANCEL); }

    @Override public void assign(Incident i, Technician t, Instant at) { doAssign(i, t, at); }
    @Override public void cancel(Incident i, String reason, Instant at) { doCancel(i, reason, at); }
}
