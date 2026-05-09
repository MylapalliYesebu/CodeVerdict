package com.codeverdict.server;

/**
 * Central configuration constants for the CodeVerdict HTTP server.
 *
 * <p>All tuneable values are declared here to avoid magic numbers scattered
 * across the codebase.  Runtime overrides via environment variables are
 * handled in {@link HttpServerFactory}.
 *
 * <p>This class is a pure constants holder and cannot be instantiated.
 */
public final class ServerConfig {

    /** Prevent instantiation. */
    private ServerConfig() {}

    /**
     * Default TCP port the HTTP server listens on when the {@code PORT}
     * environment variable is absent or invalid.
     */
    public static final int DEFAULT_PORT = 8080;

    /**
     * Number of threads in the server's fixed thread pool when the
     * {@code THREAD_POOL_SIZE} environment variable is absent or invalid.
     *
     * <p>A value of 10 is suitable for moderate concurrency.  Increase for
     * high-traffic deployments.
     */
    public static final int THREAD_POOL_SIZE = 10;

    /**
     * Maximum allowed size of an incoming HTTP request body, in bytes (1 MB).
     *
     * <p>Handlers should reject payloads exceeding this limit with
     * {@code 413 Payload Too Large}.
     */
    public static final int MAX_REQUEST_BODY = 1024 * 1024; // 1 MB
}
