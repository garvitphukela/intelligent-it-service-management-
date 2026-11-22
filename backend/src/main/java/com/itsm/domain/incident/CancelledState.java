package com.itsm.domain.incident;

import java.util.EnumSet;
import java.util.Set;

/** Terminal state for incidents withdrawn before work started. */
final class CancelledState extends AbstractIncidentState {
    @Override public IncidentStatus status() { return IncidentStatus.CANCELLED; }
    @Override public Set<IncidentStatus> allowedTransitions() { return EnumSet.noneOf(IncidentStatus.class); }
    @Override public Set<IncidentAction> supportedActions() { return EnumSet.noneOf(IncidentAction.class); }
}
