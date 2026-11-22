package com.itsm.exception;

public class WorkloadExceededException extends ItsmException {

    public WorkloadExceededException(String message) {
        super(message);
    }

    @Override
    public String getCode() { return "WORKLOAD_EXCEEDED"; }

    @Override
    public int getHttpStatus() { return 409; }
}
