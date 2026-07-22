package com.tickets.managementtickets.shared.application.exception;

public class ConflictException extends ApplicationException {

    public ConflictException(String code, String message) {
        super(code, ApplicationErrorStatus.CONFLICT, message);
    }
}
