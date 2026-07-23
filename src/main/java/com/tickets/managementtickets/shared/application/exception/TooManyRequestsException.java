package com.tickets.managementtickets.shared.application.exception;

public class TooManyRequestsException extends ApplicationException {

    public TooManyRequestsException(String code, String message) {
        super(code, ApplicationErrorStatus.TOO_MANY_REQUESTS, message);
    }
}
