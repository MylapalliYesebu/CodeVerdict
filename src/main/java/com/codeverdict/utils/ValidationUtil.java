package com.codeverdict.utils;

import java.util.regex.Pattern;

/**
 * Security utility for validating and sanitizing user input.
 * All user input must be sanitized or validated before interacting with the database.
 */
public final class ValidationUtil {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{3,20}$");
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[a-zA-Z])(?=.*\\d).{8,100}$");

    private ValidationUtil() {}

    /**
     * Checks if the email matches standard email structure.
     */
    public static boolean isValidEmail(String email) {
        return isNotEmpty(email) && EMAIL_PATTERN.matcher(email).matches();
    }

    /**
     * Checks if the username is 3-20 characters long and contains only alphanumeric characters or underscores.
     */
    public static boolean isValidUsername(String username) {
        return isNotEmpty(username) && USERNAME_PATTERN.matcher(username).matches();
    }

    /**
     * Checks if the password is 8-100 characters long, containing at least one letter and one digit.
     */
    public static boolean isValidPassword(String password) {
        return isNotEmpty(password) && PASSWORD_PATTERN.matcher(password).matches();
    }

    /**
     * Returns true if the string is not null and not blank.
     */
    public static boolean isNotEmpty(String str) {
        return str != null && !str.isBlank();
    }

    /**
     * Trims whitespace and caps the length of the string to 10000 characters to prevent huge payload storage.
     */
    public static String sanitizeString(String str) {
        if (str == null) {
            return null;
        }
        String trimmed = str.trim();
        return trimmed.length() > 10000 ? trimmed.substring(0, 10000) : trimmed;
    }

    /**
     * Validates problem difficulty values.
     */
    public static boolean isValidDifficulty(String difficulty) {
        return "EASY".equals(difficulty) || "MEDIUM".equals(difficulty) || "HARD".equals(difficulty);
    }

    /**
     * Validates programming language values (currently only supports JAVA).
     */
    public static boolean isValidLanguage(String language) {
        return "JAVA".equalsIgnoreCase(language);
    }
}
