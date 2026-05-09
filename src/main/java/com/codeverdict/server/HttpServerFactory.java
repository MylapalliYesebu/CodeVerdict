package com.codeverdict.server;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Factory that creates and configures a {@link HttpServer} instance.
 *
 * <p>The factory binds the server to the requested port, wires up a fixed
 * thread-pool executor, and returns the fully configured server without
 * starting it.  The caller is responsible for registering contexts and
 * invoking {@link HttpServer#start()}.
 *
 * <p><strong>Thread-pool tuning:</strong> Set the {@code THREAD_POOL_SIZE}
 * environment variable (or system property) to override the default defined
 * in {@link ServerConfig#THREAD_POOL_SIZE}.
 */
public final class HttpServerFactory {

    private static final Logger LOGGER = Logger.getLogger(HttpServerFactory.class.getName());

    /** Name of the env/system-property key used to override thread pool size. */
    private static final String THREAD_POOL_ENV_KEY = "THREAD_POOL_SIZE";

    /** Prevent instantiation. */
    private HttpServerFactory() {}

    /**
     * Creates a configured {@link HttpServer} bound to {@code port}.
     *
     * <ul>
     *   <li>Socket backlog: 50 connections.</li>
     *   <li>Executor: fixed thread pool (size from {@code THREAD_POOL_SIZE}
     *       env/property, falling back to {@link ServerConfig#THREAD_POOL_SIZE}).</li>
     * </ul>
     *
     * <p>If the port is already in use, the resulting {@link java.net.BindException}
     * propagates to the caller — it is not swallowed here.
     *
     * @param port TCP port to bind; must be in range 1–65535
     * @return a fully configured but <em>not yet started</em> {@link HttpServer}
     * @throws IOException if the server socket cannot be created or bound
     */
    public static HttpServer createServer(int port) throws IOException {
        int threadPoolSize = resolveThreadPoolSize();

        InetSocketAddress address = new InetSocketAddress(port);
        HttpServer server = HttpServer.create(address, /*backlog=*/ 50);
        server.setExecutor(Executors.newFixedThreadPool(threadPoolSize));

        LOGGER.log(Level.INFO,
                "HTTP server bound to {0}:{1} with thread pool size {2}.",
                new Object[]{address.getHostString(), port, threadPoolSize});

        return server;
    }

    // ----------------------------------------------------------------------- //
    //  Private helpers                                                         //
    // ----------------------------------------------------------------------- //

    /**
     * Reads {@code THREAD_POOL_SIZE} from environment variables or system
     * properties (checked in that order).  Falls back to
     * {@link ServerConfig#THREAD_POOL_SIZE} on a missing or non-numeric value.
     *
     * @return the resolved thread pool size (always ≥ 1)
     */
    private static int resolveThreadPoolSize() {
        // Prefer real env variable; fall back to system property set by EnvConfig
        String raw = System.getenv(THREAD_POOL_ENV_KEY);
        if (raw == null || raw.isBlank()) {
            raw = System.getProperty(THREAD_POOL_ENV_KEY);
        }

        if (raw == null || raw.isBlank()) {
            return ServerConfig.THREAD_POOL_SIZE;
        }

        try {
            int size = Integer.parseInt(raw.trim());
            if (size < 1) {
                throw new NumberFormatException("must be >= 1");
            }
            return size;
        } catch (NumberFormatException e) {
            LOGGER.log(Level.WARNING,
                    "Invalid THREAD_POOL_SIZE value \"{0}\" — defaulting to {1}.",
                    new Object[]{raw, ServerConfig.THREAD_POOL_SIZE});
            return ServerConfig.THREAD_POOL_SIZE;
        }
    }
}
