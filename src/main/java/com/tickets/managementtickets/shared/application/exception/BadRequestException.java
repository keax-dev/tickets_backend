package com.tickets.managementtickets.shared.application.exception;

import org.springframework.http.HttpStatus;

public class BadRequestException extends ApplicationException {

    public BadRequestException(String code, String message) {
        super(code, HttpStatus.BAD_REQUEST, message);
    }
}
