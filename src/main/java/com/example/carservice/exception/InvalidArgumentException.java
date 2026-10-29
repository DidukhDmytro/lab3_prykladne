package com.example.carservice.exception;

/**
 * Signals that an argument violates a domain requirement.
 */
public class InvalidArgumentException extends DomainException {
    public InvalidArgumentException(String message) {
        super(message);
    }
}
