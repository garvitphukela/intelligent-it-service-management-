package com.itsm.exception;

public class ResourceNotFoundException extends ItsmException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    @Override
    public String getCode() { return "NOT_FOUND"; }

    @Override
    public int getHttpStatus() { return 404; }
}
