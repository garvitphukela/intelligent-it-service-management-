package com.itsm.exception;

public class InvalidLoginException extends ItsmException {

    public InvalidLoginException(String message) {
        super(message);
    }

    @Override
    public String getCode() { return "INVALID_LOGIN"; }

    @Override
    public int getHttpStatus() { return 401; }
}
