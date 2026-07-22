package com.tickets.managementtickets.shared.application.exception;

public class BadRequestException extends ApplicationException {

    public BadRequestException(String code, String message) {
        super(code, ApplicationErrorStatus.BAD_REQUEST, message);
    }
}
