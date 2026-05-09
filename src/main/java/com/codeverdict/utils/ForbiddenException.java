package com.codeverdict.utils;

public class ForbiddenException extends CodeVerdictException {
    public ForbiddenException(String message) {
        super(message, 403);
    }
}
