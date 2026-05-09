package com.codeverdict.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Centralised JSON utility backed by a single shared {@link Gson} instance.
 *
 * <p>All serialisation and deserialisation in CodeVerdict must go through
 * this class — never instantiate {@link Gson} directly in handlers or
 * services.
 *
 * <p>The shared instance is configured with pretty-printing enabled, which
 * aids readability during development. For production bandwidth savings,
 * swap {@link GsonBuilder#setPrettyPrinting()} out for a compact instance.
 */
public final class JsonUtil {

    private static final Logger LOGGER = Logger.getLogger(JsonUtil.class.getName());

    /**
     * Shared, thread-safe {@link Gson} instance with pretty-printing enabled.
     * {@link Gson} is thread-safe after construction, so a single instance is
     * safe to use concurrently across all request-handling threads.
     */
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()   // keeps <, >, & readable in JSON output
            .create();

    /** Prevent instantiation. */
    private JsonUtil() {}

    // ------------------------------------------------------------------ //
    //  Serialisation                                                       //
    // ------------------------------------------------------------------ //

    /**
     * Serialises {@code obj} to a JSON string.
     *
     * @param obj the object to serialise; {@code null} produces {@code "null"}
     * @return JSON representation of {@code obj}
     */
    public static String toJson(Object obj) {
        return GSON.toJson(obj);
    }

    // ------------------------------------------------------------------ //
    //  Deserialisation                                                     //
    // ------------------------------------------------------------------ //

    /**
     * Deserialises a JSON string into an instance of {@code clazz}.
     *
     * <p>Returns {@code null} if {@code json} is {@code null}, blank, or
     * cannot be parsed as the requested type.
     *
     * @param <T>   the target type
     * @param json  the JSON string to parse
     * @param clazz the target class
     * @return a populated instance of {@code T}, or {@code null} on failure
     */
    public static <T> T fromJson(String json, Class<T> clazz) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return GSON.fromJson(json, clazz);
        } catch (JsonSyntaxException e) {
            LOGGER.log(Level.WARNING, "Failed to parse JSON into {0}: {1}",
                    new Object[]{clazz.getSimpleName(), e.getMessage()});
            return null;
        }
    }

    // ------------------------------------------------------------------ //
    //  Convenience builders                                                //
    // ------------------------------------------------------------------ //

    /**
     * Builds a compact JSON error envelope.
     *
     * <p>Output format: {@code {"error":"<message>"}}
     *
     * @param message the human-readable error description
     * @return JSON error string
     */
    public static String createErrorJson(String message) {
        String safe = (message != null) ? message.replace("\"", "'") : "Unknown error";
        return "{\"error\":\"" + safe + "\"}";
    }

    /**
     * Builds a compact JSON success envelope.
     *
     * <p>Output format: {@code {"message":"<message>"}}
     *
     * @param message the human-readable success description
     * @return JSON success string
     */
    public static String createSuccessJson(String message) {
        String safe = (message != null) ? message.replace("\"", "'") : "";
        return "{\"message\":\"" + safe + "\"}";
    }
}
