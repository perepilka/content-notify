package com.perepilka.coreservice.exception;

public class DuplicateSubscriptionException extends RuntimeException {
    
    public DuplicateSubscriptionException(String message) {
        super(message);
    }
}
