package com.itsm.exception;

public class ServiceNotFoundException extends ItsmException {

    public ServiceNotFoundException(String message) {
        super(message);
    }

    @Override
    public String getCode() { return "SERVICE_NOT_FOUND"; }

    @Override
    public int getHttpStatus() { return 404; }
}
