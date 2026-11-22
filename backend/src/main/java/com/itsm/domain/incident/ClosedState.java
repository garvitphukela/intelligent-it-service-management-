package com.itsm.domain.incident;

import java.util.EnumSet;
import java.util.Set;

/** Terminal state: every action is rejected. */
final class ClosedState extends AbstractIncidentState {
    @Override public IncidentStatus status() { return IncidentStatus.CLOSED; }
    @Override public Set<IncidentStatus> allowedTransitions() { return EnumSet.noneOf(IncidentStatus.class); }
    @Override public Set<IncidentAction> supportedActions() { return EnumSet.noneOf(IncidentAction.class); }
}
