package com.itsm.domain.incident;

/** Lifecycle actions understood by the state machine. */
public enum IncidentAction {
    CLASSIFY, ASSIGN, ACKNOWLEDGE, START_WORK, REQUEST_INFO, PROVIDE_INFO, RESOLVE, VERIFY, REOPEN, CLOSE, CANCEL
}
