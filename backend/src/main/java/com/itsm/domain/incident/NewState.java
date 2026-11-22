package com.itsm.domain.incident;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;

final class NewState extends AbstractIncidentState {
    @Override public IncidentStatus status() { return IncidentStatus.NEW; }
    @Override public Set<IncidentStatus> allowedTransitions() { return EnumSet.of(IncidentStatus.CLASSIFIED, IncidentStatus.CANCELLED); }
    @Override public Set<IncidentAction> supportedActions() { return EnumSet.of(IncidentAction.CLASSIFY, IncidentAction.CANCEL); }

    @Override
    public void classify(Incident i, Instant at) {
        if (i.getCategory() == null || i.getPriority() == null) {
            throw new IllegalStateException("Category and priority must be determined before classification");
        }
        i.moveTo(IncidentStatus.CLASSIFIED, at);
    }

    @Override public void cancel(Incident i, String reason, Instant at) { doCancel(i, reason, at); }
}
