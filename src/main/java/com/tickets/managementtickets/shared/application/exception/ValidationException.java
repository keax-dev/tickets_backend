package com.tickets.managementtickets.shared.application.exception;

public class ValidationException extends ApplicationException {

    public ValidationException(String code, String message) {
        super(code, ApplicationErrorStatus.UNPROCESSABLE_CONTENT, message);
    }
}
