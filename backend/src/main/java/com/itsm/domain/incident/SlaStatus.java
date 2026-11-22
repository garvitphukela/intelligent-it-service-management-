package com.itsm.domain.incident;

public enum SlaStatus {
    /** Clock running, comfortably inside the window. */
    ON_TRACK,
    /** Past the warning threshold or predicted to breach. */
    AT_RISK,
    /** A response or resolution deadline was missed. */
    BREACHED,
    /** Resolved within the agreed window. */
    MET,
    /** Clock paused while waiting for the reporter. */
    PAUSED,
    /** No SLA applies (e.g. cancelled). */
    NOT_APPLICABLE
}
