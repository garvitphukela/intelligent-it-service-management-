package com.itsm.domain.incident;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;

/** Waiting for the reporter; the resolution SLA clock is paused. */
final class AwaitingInfoState extends AbstractIncidentState {
    @Override public IncidentStatus status() { return IncidentStatus.AWAITING_INFO; }
    @Override public Set<IncidentStatus> allowedTransitions() { return EnumSet.of(IncidentStatus.IN_PROGRESS); }
    @Override public Set<IncidentAction> supportedActions() { return EnumSet.of(IncidentAction.PROVIDE_INFO); }

    @Override
    public void provideInfo(Incident i, Instant at) {
        i.getSla().resume(at);
        i.moveTo(IncidentStatus.IN_PROGRESS, at);
    }
}
