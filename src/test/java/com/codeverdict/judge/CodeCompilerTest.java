package com.codeverdict.judge;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CodeCompilerTest {

    private CodeCompiler compiler;

    @BeforeEach
    void setUp() {
        compiler = new CodeCompiler();
    }

    @Test
    @DisplayName("Compile valid HelloWorld -> success")
    void shouldReturnSuccess_whenValidHelloWorld() {
        String code = "public class HelloWorld { public static void main(String[] args) { System.out.println(\"Hello\"); } }";
        ExecutionResult result = compiler.compile("test-submit-1", code);
        
        assertTrue(result.isSuccess(), "Compilation should succeed");
        assertEquals("HelloWorld", result.getOutput());
        
        compiler.cleanup("test-submit-1");
    }

    @Test
    @DisplayName("Compile with syntax error -> failure with error message")
    void shouldReturnFailure_whenSyntaxError() {
        String code = "public class BadCode { public static void main(String[] args) { System.out.println(\"Hello) } }";
        ExecutionResult result = compiler.compile("test-submit-2", code);
        
        assertFalse(result.isSuccess(), "Compilation should fail");
        assertTrue(result.getErrorOutput().contains("error") || result.getErrorOutput().contains("unclosed string literal"), "Error output should contain compile errors");
        
        compiler.cleanup("test-submit-2");
    }
}
