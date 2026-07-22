package com.tickets.managementtickets.shared.application.exception;

public class NotFoundException extends ApplicationException {

    public NotFoundException(String code, String message) {
        super(code, ApplicationErrorStatus.NOT_FOUND, message);
    }
}
