package com.tickets.managementtickets.shared.application.exception;

import org.springframework.http.HttpStatus;

public class ValidationException extends ApplicationException {

    public ValidationException(String code, String message) {
        super(code, HttpStatus.UNPROCESSABLE_CONTENT, message);
    }
}
