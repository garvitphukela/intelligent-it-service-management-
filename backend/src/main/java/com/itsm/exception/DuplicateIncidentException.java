package com.itsm.exception;

public class DuplicateIncidentException extends ItsmException {

    public DuplicateIncidentException(String message) {
        super(message);
    }

    @Override
    public String getCode() { return "DUPLICATE_INCIDENT"; }

    @Override
    public int getHttpStatus() { return 409; }
}
