package com.codeverdict.utils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility class that loads key-value pairs from a {@code .env} file in the
 * working directory and injects them as system properties.
 *
 * <p>Lines starting with {@code #} are treated as comments and ignored.
 * Lines without an {@code =} separator are also silently skipped.
 *
 * <p>Example {@code .env} line:
 * <pre>
 *   PORT=8080
 *   DATABASE_URL=jdbc:postgresql://localhost:5432/codeverdict
 * </pre>
 */
public final class EnvConfig {

    private static final Logger LOGGER = Logger.getLogger(EnvConfig.class.getName());
    private static final String ENV_FILE = ".env";

    static {
        int count = 0;
        if (Files.exists(Paths.get(ENV_FILE))) {
            try (BufferedReader reader = new BufferedReader(new FileReader(ENV_FILE))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("#")) {
                        continue;
                    }
                    int idx = line.indexOf('=');
                    if (idx > 0) {
                        String key = line.substring(0, idx).trim();
                        String value = line.substring(idx + 1).trim();
                        System.setProperty(key, value);
                        count++;
                    }
                }
                LOGGER.info("Loaded " + count + " environment variables from .env");
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Could not read .env file: {0}", e.getMessage());
            }
        }
    }

    private EnvConfig() {}

    public static void load() {
        validate();
    }

    public static void validate() {
        require("DB_URL");
        require("DB_USER");
        require("DB_PASSWORD");
        String jwt = require("JWT_SECRET");
        if (jwt.length() < 32) {
            throw new IllegalStateException("JWT_SECRET must be at least 32 characters long");
        }
    }

    public static String get(String key) {
        String val = System.getenv(key);
        if (val != null) return val;
        return System.getProperty(key);
    }

    public static String get(String key, String defaultValue) {
        String val = get(key);
        return (val != null) ? val : defaultValue;
    }

    public static int getInt(String key, int defaultValue) {
        String val = get(key);
        if (val == null) return defaultValue;
        try {
            return Integer.parseInt(val);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public static boolean getBoolean(String key, boolean defaultValue) {
        String val = get(key);
        if (val == null) return defaultValue;
        return Boolean.parseBoolean(val);
    }

    public static String require(String key) {
        String val = get(key);
        if (val == null || val.isBlank()) {
            throw new IllegalStateException("Missing required environment variable: " + key + ". Check your .env file.");
        }
        return val;
    }
}
