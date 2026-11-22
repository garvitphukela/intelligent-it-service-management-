package com.itsm.domain.incident;

import java.util.EnumSet;
import java.util.Set;

public enum IncidentStatus {
    NEW, CLASSIFIED, ASSIGNED, ACKNOWLEDGED, IN_PROGRESS, AWAITING_INFO, RESOLVED, VERIFIED, CLOSED, CANCELLED;

    private static final Set<IncidentStatus> ACTIVE_WORK = EnumSet.of(ASSIGNED, ACKNOWLEDGED, IN_PROGRESS, AWAITING_INFO);
    private static final Set<IncidentStatus> TERMINAL = EnumSet.of(CLOSED, CANCELLED);
    private static final Set<IncidentStatus> DONE = EnumSet.of(RESOLVED, VERIFIED, CLOSED, CANCELLED);

    /** Counts towards a technician's current workload. */
    public boolean isActiveWork() { return ACTIVE_WORK.contains(this); }

    /** Not yet resolved (SLA clock may still be running). */
    public boolean isOpen() { return !DONE.contains(this); }

    public boolean isTerminal() { return TERMINAL.contains(this); }

    public static Set<IncidentStatus> openStatuses() { return EnumSet.complementOf(EnumSet.copyOf(DONE)); }

    public static Set<IncidentStatus> activeWorkStatuses() { return EnumSet.copyOf(ACTIVE_WORK); }
}
