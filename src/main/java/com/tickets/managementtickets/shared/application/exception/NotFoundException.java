package com.tickets.managementtickets.shared.application.exception;

import org.springframework.http.HttpStatus;

public class NotFoundException extends ApplicationException {

    public NotFoundException(String code, String message) {
        super(code, HttpStatus.NOT_FOUND, message);
    }
}
