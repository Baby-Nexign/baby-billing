package org.babynexign.babybilling.brtservice.exception;

public class SubscriberNotFoundException extends RuntimeException {
    public SubscriberNotFoundException(String message) {
        super(message);
    }
}