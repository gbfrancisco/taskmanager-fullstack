package com.tutorial.taskmanager.exception;

import java.util.NoSuchElementException;

public class ResourceNotFoundException extends NoSuchElementException {
    public ResourceNotFoundException(String resource, Long id) {
        super("%s with id '%s' not found".formatted(resource, id));
    }

    public ResourceNotFoundException(String resource, String field, String value) {
        super("%s with %s '%s' not found".formatted(resource, field, value));
    }

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
