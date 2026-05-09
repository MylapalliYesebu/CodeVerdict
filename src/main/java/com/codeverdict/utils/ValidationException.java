package com.codeverdict.utils;

public class ValidationException extends CodeVerdictException {
    public ValidationException(String message) {
        super(message, 400);
    }
}
