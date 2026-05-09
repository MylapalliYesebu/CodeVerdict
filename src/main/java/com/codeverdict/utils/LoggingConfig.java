package com.codeverdict.utils;

import java.io.InputStream;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

public class LoggingConfig {
    public static void init() {
        try (InputStream is = LoggingConfig.class.getResourceAsStream("/logging.properties")) {
            if (is != null) {
                LogManager.getLogManager().readConfiguration(is);
            }
        } catch (Exception e) {
            System.err.println("Failed to load logging.properties: " + e.getMessage());
        }

        Logger rootLogger = LogManager.getLogManager().getLogger("");
        
        String logLevelEnv = System.getenv("LOG_LEVEL");
        if (logLevelEnv != null && !logLevelEnv.isBlank()) {
            try {
                Level level = Level.parse(logLevelEnv.trim().toUpperCase());
                rootLogger.setLevel(level);
            } catch (IllegalArgumentException e) {
                System.err.println("Invalid LOG_LEVEL env var: " + logLevelEnv);
            }
        }

        String logFile = System.getenv("LOG_FILE");
        if (logFile != null && !logFile.isBlank()) {
            try {
                FileHandler fileHandler = new FileHandler(logFile, 10 * 1024 * 1024, 5, true);
                fileHandler.setFormatter(new java.util.logging.SimpleFormatter());
                rootLogger.addHandler(fileHandler);
            } catch (Exception e) {
                System.err.println("Failed to setup FileHandler: " + e.getMessage());
            }
        }
    }
}
