package com.itsm.exception;

public class IncidentNotFoundException extends ItsmException {

    public IncidentNotFoundException(String message) {
        super(message);
    }

    @Override
    public String getCode() { return "INCIDENT_NOT_FOUND"; }

    @Override
    public int getHttpStatus() { return 404; }
}
