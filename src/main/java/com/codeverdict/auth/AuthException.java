package com.codeverdict.auth;

import com.codeverdict.utils.CodeVerdictException;

/**
 * Exception thrown when authentication fails.
 */
public class AuthException extends CodeVerdictException {
    
    public AuthException(String message) {
        super(message, 401);
    }
}
