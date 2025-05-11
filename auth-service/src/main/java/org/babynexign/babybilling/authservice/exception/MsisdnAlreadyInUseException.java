package org.babynexign.babybilling.authservice.exception;

public class MsisdnAlreadyInUseException extends RuntimeException {
    public MsisdnAlreadyInUseException(String message) {
        super(message);
    }
}