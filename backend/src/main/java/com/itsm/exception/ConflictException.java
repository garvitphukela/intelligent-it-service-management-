package com.itsm.exception;

public class ConflictException extends ItsmException {

    public ConflictException(String message) {
        super(message);
    }

    @Override
    public String getCode() { return "CONFLICT"; }

    @Override
    public int getHttpStatus() { return 409; }
}
