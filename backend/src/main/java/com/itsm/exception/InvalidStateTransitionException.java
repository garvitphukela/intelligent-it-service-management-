package com.itsm.exception;

/** Thrown by the incident State pattern when an action is not allowed in the current state. */
public class InvalidStateTransitionException extends ItsmException {

    private final String currentState;
    private final String attemptedAction;

    public InvalidStateTransitionException(String currentState, String attemptedAction) {
        super("Cannot '" + attemptedAction + "' an incident that is " + currentState);
        this.currentState = currentState;
        this.attemptedAction = attemptedAction;
    }

    public String getCurrentState() { return currentState; }
    public String getAttemptedAction() { return attemptedAction; }

    @Override
    public String getCode() { return "INVALID_STATE_TRANSITION"; }

    @Override
    public int getHttpStatus() { return 409; }
}
