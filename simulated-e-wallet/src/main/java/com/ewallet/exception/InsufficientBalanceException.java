package com.ewallet.exception;

/**
 * Thrown when a wallet does not have sufficient funds to complete a transfer.
 * Maps to HTTP 400 Bad Request.
 */
public class InsufficientBalanceException extends RuntimeException {
    public InsufficientBalanceException(String message) {
        super(message);
    }
}
