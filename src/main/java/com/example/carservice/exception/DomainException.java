package com.example.carservice.exception;

/**
 * Base exception for business rule violations in the car service domain.
 */
public class DomainException extends RuntimeException {
    private static long serialVersionUID = 1L;

    public DomainException(String message) {
        super(message);
    }

    public DomainException(String message, Throwable cause) {
        super(message, cause);
    }
}
