package com.tickets.managementtickets.shared.application.exception;

public class ApplicationException extends RuntimeException {

    private final String code;
    private final ApplicationErrorStatus status;

    public ApplicationException(String code, ApplicationErrorStatus status, String message) {
        super(message);
        this.code = code;
        this.status = status;
    }

    public String getCode() {
        return code;
    }

    public ApplicationErrorStatus getStatus() {
        return status;
    }
}
