package com.ewallet.exception;

/**
 * Thrown when an idempotency key is missing, malformed, or invalid.
 * Maps to HTTP 400 Bad Request.
 */
public class InvalidIdempotencyKeyException extends RuntimeException {
    public InvalidIdempotencyKeyException(String message) {
        super(message);
    }
}
