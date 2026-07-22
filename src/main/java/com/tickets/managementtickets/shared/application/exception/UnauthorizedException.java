package com.tickets.managementtickets.shared.application.exception;

public class UnauthorizedException extends ApplicationException {

    public UnauthorizedException(String code, String message) {
        super(code, ApplicationErrorStatus.UNAUTHORIZED, message);
    }
}
