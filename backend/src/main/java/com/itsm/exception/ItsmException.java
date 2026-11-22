package com.itsm.exception;

/**
 * Root of the application's exception hierarchy. Each subclass carries a stable
 * machine-readable {@link #getCode() code} and an HTTP status so the global handler can
 * translate any domain failure into a consistent API error without instanceof chains.
 */
public abstract class ItsmException extends RuntimeException {

    protected ItsmException(String message) {
        super(message);
    }

    protected ItsmException(String message, Throwable cause) {
        super(message, cause);
    }

    /** Stable error code, e.g. INVALID_STATE_TRANSITION. */
    public abstract String getCode();

    /** HTTP status code to return. */
    public abstract int getHttpStatus();
}
