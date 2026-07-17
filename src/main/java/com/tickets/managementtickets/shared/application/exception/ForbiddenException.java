package com.tickets.managementtickets.shared.application.exception;

import org.springframework.http.HttpStatus;

public class ForbiddenException extends ApplicationException {

    public ForbiddenException(String code, String message) {
        super(code, HttpStatus.FORBIDDEN, message);
    }
}
