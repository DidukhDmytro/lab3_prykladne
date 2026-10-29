package com.example.carservice.exception;

/**
 * Signals that a mechanic is not compatible with the requested work.
 */
public class MechanicNotCompatibleException extends DomainException {
    public MechanicNotCompatibleException(String message) {
        super(message);
    }
}
