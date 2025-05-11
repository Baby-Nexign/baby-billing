package org.babynexign.babybilling.brtservice.exception;

public class MsisdnAlreadyExistsException extends RuntimeException {
    public MsisdnAlreadyExistsException(String message) {
        super(message);
    }
}