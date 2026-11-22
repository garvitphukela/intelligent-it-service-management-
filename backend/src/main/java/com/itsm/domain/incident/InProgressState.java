package com.itsm.domain.incident;

import com.itsm.domain.user.Technician;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;

final class InProgressState extends AbstractIncidentState {
    @Override public IncidentStatus status() { return IncidentStatus.IN_PROGRESS; }
    @Override public Set<IncidentStatus> allowedTransitions() {
        return EnumSet.of(IncidentStatus.AWAITING_INFO, IncidentStatus.RESOLVED, IncidentStatus.ASSIGNED);
    }
    @Override public Set<IncidentAction> supportedActions() {
        return EnumSet.of(IncidentAction.REQUEST_INFO, IncidentAction.RESOLVE, IncidentAction.ASSIGN);
    }

    @Override
    public void requestInfo(Incident i, Instant at) {
        i.getSla().pause(at);
        i.moveTo(IncidentStatus.AWAITING_INFO, at);
    }

    @Override
    public void resolve(Incident i, Resolution r, Instant at) {
        if (r == null) throw new IllegalArgumentException("Resolution details are required");
        i.recordResolution(r, at);
        i.moveTo(IncidentStatus.RESOLVED, at);
    }

    @Override public void assign(Incident i, Technician t, Instant at) { doAssign(i, t, at); }
}
