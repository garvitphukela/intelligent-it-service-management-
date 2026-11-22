package com.itsm.exception;

public class UnauthorizedOperationException extends ItsmException {

    public UnauthorizedOperationException(String message) {
        super(message);
    }

    @Override
    public String getCode() { return "UNAUTHORIZED_OPERATION"; }

    @Override
    public int getHttpStatus() { return 403; }
}
