package com.codeverdict.utils;

import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GlobalExceptionMapper {
    private static final Logger LOGGER = Logger.getLogger(GlobalExceptionMapper.class.getName());

    public static void handle(HttpExchange ex, Throwable t) {
        int status = 500;
        String message = "Internal server error. Please try again.";
        Level logLevel = Level.SEVERE;
        boolean logStackTrace = true;

        if (t instanceof CodeVerdictException) {
            CodeVerdictException cve = (CodeVerdictException) t;
            status = cve.getStatusCode();
            message = cve.getMessage();
            logLevel = Level.WARNING;
            logStackTrace = false;
        } else if (t instanceof IllegalArgumentException) {
            status = 400;
            message = t.getMessage() != null ? t.getMessage() : "Invalid arguments";
            logLevel = Level.WARNING;
            logStackTrace = false;
        } else if (t instanceof RuntimeException) {
            String originalMessage = t.getMessage();
            String msg = originalMessage != null ? originalMessage.toLowerCase() : "";
            if (msg.contains("duplicate") || msg.contains("unique")) {
                status = 409;
                message = "A resource with these details already exists";
                logLevel = Level.WARNING;
                logStackTrace = false;
            } else if (msg.contains("malformed json") || msg.contains("syntax")) {
                status = 400;
                message = "Invalid data format";
                logLevel = Level.WARNING;
                logStackTrace = false;
            }
        }

        if (logStackTrace) {
            LOGGER.log(logLevel, "Unhandled exception during request processing", t);
        } else {
            LOGGER.log(logLevel, "Handled exception [{0}]: {1}", new Object[]{status, t.getMessage()});
        }

        sendErrorResponse(ex, status, message);
    }

    private static void sendErrorResponse(HttpExchange ex, int status, String message) {
        String safeMessage = (message != null) ? message.replace("\"", "'") : "Unknown error";
        String body = String.format("{\"error\":\"%s\",\"status\":%d,\"timestamp\":\"%s\"}", 
                safeMessage, status, Instant.now().toString());

        try {
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            ex.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
            ex.sendResponseHeaders(status, bytes.length);
            try (OutputStream os = ex.getResponseBody()) {
                os.write(bytes);
            }
        } catch (IOException e) {
            LOGGER.severe("Failed to send error response: " + e.getMessage());
        } finally {
            ex.close();
        }
    }
}
