package com.itsm.domain.incident;

import com.itsm.domain.user.Technician;
import com.itsm.exception.InvalidStateTransitionException;

import java.time.Instant;

/**
 * Default behaviour: every action is rejected. Concrete states override only the
 * actions that are legal for them, which keeps each state class small and explicit.
 */
abstract class AbstractIncidentState implements IncidentState {

    protected InvalidStateTransitionException reject(String action) {
        return new InvalidStateTransitionException(status().name(), action);
    }

    @Override public void classify(Incident i, Instant at) { throw reject("classify"); }
    @Override public void assign(Incident i, Technician t, Instant at) { throw reject("assign"); }
    @Override public void acknowledge(Incident i, Instant at) { throw reject("acknowledge"); }
    @Override public void startWork(Incident i, Instant at) { throw reject("start work on"); }
    @Override public void requestInfo(Incident i, Instant at) { throw reject("request information for"); }
    @Override public void provideInfo(Incident i, Instant at) { throw reject("provide information for"); }
    @Override public void resolve(Incident i, Resolution r, Instant at) { throw reject("resolve"); }
    @Override public void verify(Incident i, Instant at) { throw reject("verify"); }
    @Override public void reopen(Incident i, Instant at) { throw reject("reopen"); }
    @Override public void close(Incident i, String notes, String by, Instant at) { throw reject("close"); }
    @Override public void cancel(Incident i, String reason, Instant at) { throw reject("cancel"); }

    /** Shared helper: (re)assign and move to ASSIGNED. */
    protected void doAssign(Incident i, Technician t, Instant at) {
        i.recordAssignment(t);
        i.moveTo(IncidentStatus.ASSIGNED, at);
    }

    protected void doCancel(Incident i, String reason, Instant at) {
        if (reason == null || reason.isBlank()) throw new IllegalArgumentException("A cancellation reason is required");
        i.recordCancellation(reason);
        i.moveTo(IncidentStatus.CANCELLED, at);
    }
}
