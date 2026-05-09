package com.codeverdict.utils;

public class NotFoundException extends CodeVerdictException {
    public NotFoundException(String message) {
        super(message, 404);
    }
}
