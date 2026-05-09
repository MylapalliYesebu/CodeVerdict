package com.codeverdict.utils;

public class CodeVerdictException extends RuntimeException {
    private final int statusCode;

    public CodeVerdictException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
