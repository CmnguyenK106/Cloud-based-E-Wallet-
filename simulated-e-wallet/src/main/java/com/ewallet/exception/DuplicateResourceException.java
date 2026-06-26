package com.ewallet.exception;

/**
 * Thrown when attempting to create a resource that already exists
 * (e.g., duplicate username or email during registration).
 * Maps to HTTP 409 Conflict.
 */
public class DuplicateResourceException extends RuntimeException {
    public DuplicateResourceException(String message) {
        super(message);
    }
}
