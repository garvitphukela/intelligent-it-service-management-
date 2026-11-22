package com.itsm.exception;

public class InvalidInputException extends ItsmException {

    public InvalidInputException(String message) {
        super(message);
    }

    @Override
    public String getCode() { return "INVALID_INPUT"; }

    @Override
    public int getHttpStatus() { return 400; }
}
