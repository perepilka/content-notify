package com.perepilka.coreservice.exception;

public class InvalidUrlException extends RuntimeException {
    
    public InvalidUrlException(String message) {
        super(message);
    }
}
