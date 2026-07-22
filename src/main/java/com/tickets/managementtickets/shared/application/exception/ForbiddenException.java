package com.tickets.managementtickets.shared.application.exception;

public class ForbiddenException extends ApplicationException {

    public ForbiddenException(String code, String message) {
        super(code, ApplicationErrorStatus.FORBIDDEN, message);
    }
}
