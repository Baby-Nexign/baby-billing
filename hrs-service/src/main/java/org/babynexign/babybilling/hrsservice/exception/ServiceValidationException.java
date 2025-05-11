package org.babynexign.babybilling.hrsservice.exception;

public class ServiceValidationException extends RuntimeException {
    public ServiceValidationException(String message) {
        super(message);
    }
}