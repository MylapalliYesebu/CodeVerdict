package com.codeverdict.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ValidationUtilTest {

    @Test
    @DisplayName("isValidEmail: valid, invalid format, null")
    void shouldValidateEmail() {
        assertTrue(ValidationUtil.isValidEmail("test@example.com"));
        assertFalse(ValidationUtil.isValidEmail("invalid-email"));
        assertFalse(ValidationUtil.isValidEmail(null));
        assertFalse(ValidationUtil.isValidEmail(""));
    }

    @Test
    @DisplayName("isValidUsername: valid, too short, too long, special chars")
    void shouldValidateUsername() {
        assertTrue(ValidationUtil.isValidUsername("valid_user"));
        assertFalse(ValidationUtil.isValidUsername("ab")); // too short
        assertFalse(ValidationUtil.isValidUsername("this_username_is_way_too_long_for_db")); // too long
        assertFalse(ValidationUtil.isValidUsername("invalid user!")); // special chars
        assertFalse(ValidationUtil.isValidUsername(null));
    }

    @Test
    @DisplayName("isValidPassword: valid, too short, no digit, null")
    void shouldValidatePassword() {
        assertTrue(ValidationUtil.isValidPassword("Passw0rd123"));
        assertFalse(ValidationUtil.isValidPassword("P12")); // too short
        assertFalse(ValidationUtil.isValidPassword("PasswordNoDigit")); // no digit
        assertFalse(ValidationUtil.isValidPassword(null));
    }
}
