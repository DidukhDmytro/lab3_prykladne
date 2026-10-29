package com.example.carservice.exception;

/**
 * Signals that a domain entity cannot transition to the requested state.
 */
public class InvalidStateTransitionException extends DomainException {
    public InvalidStateTransitionException(String message) {
        super(message);
    }
}
