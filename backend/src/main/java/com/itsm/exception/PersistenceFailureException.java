package com.itsm.exception;

/** Wraps low-level database failures so infrastructure details never leak to clients. */
public class PersistenceFailureException extends ItsmException {

    public PersistenceFailureException(String message, Throwable cause) {
        super(message, cause);
    }

    @Override
    public String getCode() { return "DATABASE_FAILURE"; }

    @Override
    public int getHttpStatus() { return 503; }
}
