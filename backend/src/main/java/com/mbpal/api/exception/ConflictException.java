package com.mbpal.api.exception;

public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }

    public ConflictException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format("%s already exists with %s: '%s'", resourceName, fieldName, fieldValue));
    }
}
