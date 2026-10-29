package com.example.carservice.exception;

/**
 * Signals that an entity cannot be created because it already exists.
 */
public class EntityAlreadyExistsException extends DomainException {
    public EntityAlreadyExistsException(String message) {
        super(message);
    }
}
