package com.ecommerce.common.exception;

/**
 * Thrown when attempting to register an account with an email that is already registered.
 */
public class EmailAlreadyExistsException extends RuntimeException {
    public EmailAlreadyExistsException(String message) {
        super(message);
    }
}
