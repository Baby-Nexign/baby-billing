package org.babynexign.babybilling.hrsservice.exception;

/**
 * Exception thrown when validation fails for billing operations.
 * Used for validation errors in billing requests.
 */
public class BillingValidationException extends RuntimeException {

    public BillingValidationException(String message) {
        super(message);
    }

    public BillingValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}