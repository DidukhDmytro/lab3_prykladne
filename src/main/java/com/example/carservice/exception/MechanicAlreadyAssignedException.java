package com.example.carservice.exception;

/**
 * Signals that a mechanic is already assigned to active work.
 */
public class MechanicAlreadyAssignedException extends DomainException {
    public MechanicAlreadyAssignedException(String message) {
        super(message);
    }
}
