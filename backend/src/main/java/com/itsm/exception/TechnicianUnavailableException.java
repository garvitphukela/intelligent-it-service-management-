package com.itsm.exception;

public class TechnicianUnavailableException extends ItsmException {

    public TechnicianUnavailableException(String message) {
        super(message);
    }

    @Override
    public String getCode() { return "TECHNICIAN_UNAVAILABLE"; }

    @Override
    public int getHttpStatus() { return 409; }
}
