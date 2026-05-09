package com.codeverdict.judge;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VerdictCalculatorTest {

    @Test
    @DisplayName("exact match -> ACCEPTED")
    void shouldReturnAccepted_whenOutputMatchesExactly() {
        assertEquals("ACCEPTED", VerdictCalculator.calculateVerdict("hello world", "hello world"));
    }

    @Test
    @DisplayName("match with trailing newline -> ACCEPTED (normalization)")
    void shouldReturnAccepted_whenOutputHasTrailingNewline() {
        assertEquals("ACCEPTED", VerdictCalculator.calculateVerdict("hello world\n", "hello world"));
    }

    @Test
    @DisplayName("mismatch -> WRONG_ANSWER")
    void shouldReturnWrongAnswer_whenOutputMismatches() {
        assertEquals("WRONG_ANSWER", VerdictCalculator.calculateVerdict("hello world", "hello everyone"));
    }

    @Test
    @DisplayName("empty expected vs empty actual -> ACCEPTED")
    void shouldReturnAccepted_whenBothAreEmpty() {
        assertEquals("ACCEPTED", VerdictCalculator.calculateVerdict("", ""));
        assertEquals("ACCEPTED", VerdictCalculator.calculateVerdict(null, null));
    }

    @Test
    @DisplayName("whitespace-only difference -> ACCEPTED")
    void shouldReturnAccepted_whenDifferenceIsWhitespaceOnly() {
        assertEquals("ACCEPTED", VerdictCalculator.calculateVerdict("line1 \nline2\t", "line1\nline2"));
    }
}
