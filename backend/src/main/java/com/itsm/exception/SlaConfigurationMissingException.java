package com.itsm.exception;

public class SlaConfigurationMissingException extends ItsmException {

    public SlaConfigurationMissingException(String message) {
        super(message);
    }

    @Override
    public String getCode() { return "SLA_CONFIGURATION_MISSING"; }

    @Override
    public int getHttpStatus() { return 500; }
}
