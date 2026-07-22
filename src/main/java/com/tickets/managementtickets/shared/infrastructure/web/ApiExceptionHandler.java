package com.tickets.managementtickets.shared.infrastructure.web;

import com.tickets.managementtickets.shared.application.exception.ApplicationException;
import com.tickets.managementtickets.shared.application.exception.ApplicationErrorStatus;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;
import java.util.List;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(ApplicationException.class)
    ProblemDetail handleApplicationException(ApplicationException exception, HttpServletRequest request) {
        return buildProblemDetail(toHttpStatus(exception.getStatus()), exception.getCode(), exception.getMessage(), request, List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail handleValidation(MethodArgumentNotValidException exception, HttpServletRequest request) {
        List<FieldErrorResponse> fieldErrors = exception.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(ApiExceptionHandler::mapFieldError)
            .toList();

        return buildProblemDetail(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "The request contains invalid fields.", request, fieldErrors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ProblemDetail handleConstraintViolation(ConstraintViolationException exception, HttpServletRequest request) {
        return buildProblemDetail(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", exception.getMessage(), request, List.of());
    }

    @ExceptionHandler(AccessDeniedException.class)
    ProblemDetail handleAccessDenied(AccessDeniedException exception, HttpServletRequest request) {
        return buildProblemDetail(HttpStatus.FORBIDDEN, "ACCESS_DENIED", "You do not have permission to perform this action.", request, List.of());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail handleDataIntegrityViolation(DataIntegrityViolationException exception, HttpServletRequest request) {
        return buildProblemDetail(
            HttpStatus.CONFLICT,
            "DATA_INTEGRITY_VIOLATION",
            "The request could not be completed because related data is invalid or no longer exists.",
            request,
            List.of()
        );
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnexpectedException(Exception exception, HttpServletRequest request) {
        return buildProblemDetail(HttpStatus.INTERNAL_SERVER_ERROR, "UNEXPECTED_ERROR", "An unexpected error occurred.", request, List.of());
    }

    private static FieldErrorResponse mapFieldError(FieldError error) {
        return new FieldErrorResponse(error.getField(), error.getDefaultMessage());
    }

    private HttpStatus toHttpStatus(ApplicationErrorStatus status) {
        return switch (status) {
            case BAD_REQUEST -> HttpStatus.BAD_REQUEST;
            case UNAUTHORIZED -> HttpStatus.UNAUTHORIZED;
            case FORBIDDEN -> HttpStatus.FORBIDDEN;
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case CONFLICT -> HttpStatus.CONFLICT;
            case UNPROCESSABLE_CONTENT -> HttpStatus.UNPROCESSABLE_CONTENT;
        };
    }

    private ProblemDetail buildProblemDetail(
        HttpStatus status,
        String code,
        String detail,
        HttpServletRequest request,
        List<FieldErrorResponse> fieldErrors
    ) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, detail);
        problemDetail.setTitle(status.getReasonPhrase());
        problemDetail.setType(URI.create("https://management-tickets/errors/" + code.toLowerCase().replace('_', '-')));
        problemDetail.setProperty("code", code);
        problemDetail.setProperty("correlationId", request.getAttribute(CorrelationIdFilter.CORRELATION_ID_ATTRIBUTE));
        problemDetail.setProperty("timestamp", Instant.now());
        problemDetail.setProperty("fieldErrors", fieldErrors);
        return problemDetail;
    }
}
