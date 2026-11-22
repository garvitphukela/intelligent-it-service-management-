package com.itsm.domain.incident;

import com.itsm.domain.user.Technician;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;

final class AcknowledgedState extends AbstractIncidentState {
    @Override public IncidentStatus status() { return IncidentStatus.ACKNOWLEDGED; }
    @Override public Set<IncidentStatus> allowedTransitions() { return EnumSet.of(IncidentStatus.IN_PROGRESS, IncidentStatus.ASSIGNED); }
    @Override public Set<IncidentAction> supportedActions() { return EnumSet.of(IncidentAction.START_WORK, IncidentAction.ASSIGN); }

    @Override public void startWork(Incident i, Instant at) { i.moveTo(IncidentStatus.IN_PROGRESS, at); }
    @Override public void assign(Incident i, Technician t, Instant at) { doAssign(i, t, at); }
}
