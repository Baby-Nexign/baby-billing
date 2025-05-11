package org.babynexign.babybilling.authservice.exception;

public class MsisdnVerificationException extends RuntimeException {
    public MsisdnVerificationException(String message) {
        super(message);
    }

    public MsisdnVerificationException(String message, Throwable cause) {
        super(message, cause);
    }
}