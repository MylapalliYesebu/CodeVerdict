package com.codeverdict.utils;

public class SpamSubmissionException extends CodeVerdictException {
    public SpamSubmissionException(String message) {
        super(message, 429);
    }
}
