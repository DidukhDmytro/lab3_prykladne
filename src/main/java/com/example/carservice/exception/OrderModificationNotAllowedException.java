package com.example.carservice.exception;

/**
 * Signals that an order cannot be modified in its current state.
 */
public class OrderModificationNotAllowedException extends DomainException {
    public OrderModificationNotAllowedException(String message) {
        super(message);
    }
}
