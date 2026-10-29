package com.example.carservice.exception;

/**
 * Signals that a requested entity could not be found.
 */
public class EntityNotFoundException extends DomainException {
    public EntityNotFoundException(String message) {
        super(message);
    }
}
