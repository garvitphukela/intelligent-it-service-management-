package com.itsm.domain.incident;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;

final class ResolvedState extends AbstractIncidentState {
    @Override public IncidentStatus status() { return IncidentStatus.RESOLVED; }
    @Override public Set<IncidentStatus> allowedTransitions() { return EnumSet.of(IncidentStatus.VERIFIED, IncidentStatus.IN_PROGRESS); }
    @Override public Set<IncidentAction> supportedActions() { return EnumSet.of(IncidentAction.VERIFY, IncidentAction.REOPEN); }

    @Override public void verify(Incident i, Instant at) { i.moveTo(IncidentStatus.VERIFIED, at); }

    @Override
    public void reopen(Incident i, Instant at) {
        i.clearResolution();
        i.moveTo(IncidentStatus.IN_PROGRESS, at);
    }
}
