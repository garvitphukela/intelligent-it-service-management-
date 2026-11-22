package com.itsm.exception;

/** A third-party integration (mail server, SMS gateway, AI API...) failed or was unreachable. */
public class ExternalServiceException extends ItsmException {

    public ExternalServiceException(String message, Throwable cause) {
        super(message, cause);
    }

    @Override
    public String getCode() { return "NETWORK_FAILURE"; }

    @Override
    public int getHttpStatus() { return 502; }
}
