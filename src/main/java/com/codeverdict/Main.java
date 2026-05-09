package com.codeverdict;

import com.codeverdict.database.DatabaseManager;
import com.codeverdict.routes.RouteRegistry;
import com.codeverdict.server.CorsFilter;
import com.codeverdict.server.HttpServerFactory;
import com.codeverdict.server.ServerConfig;
import com.codeverdict.utils.EnvConfig;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Entry point for the CodeVerdict backend server.
 *
 * <p>Startup sequence:
 * <ol>
 *   <li>Load environment variables from {@code .env} via {@link EnvConfig}.</li>
 *   <li>Initialise the database connection pool via {@link DatabaseManager}.</li>
 *   <li>Create a {@link HttpServer} via {@link HttpServerFactory}.</li>
 *   <li>Register all HTTP route handlers via {@link RouteRegistry}.</li>
 *   <li>Start the server and register a JVM shutdown hook for graceful stop.</li>
 * </ol>
 */
public class Main {

    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

    /**
     * Application entry point.
     *
     * @param args command-line arguments (currently unused)
     */
    public static void main(String[] args) {

        // ------------------------------------------------------------------ //
        //  1. Load environment configuration                                  //
        // ------------------------------------------------------------------ //
        EnvConfig.load();
        
        // ------------------------------------------------------------------ //
        //  1.5 Configure Logging                                              //
        // ------------------------------------------------------------------ //
        com.codeverdict.utils.LoggingConfig.init();

        // ------------------------------------------------------------------ //
        //  2. Resolve server port from environment                            //
        // ------------------------------------------------------------------ //
        int port = resolvePort();

        // ------------------------------------------------------------------ //
        //  3. Initialise database connection pool                             //
        // ------------------------------------------------------------------ //
        try {
            DatabaseManager.INSTANCE.initializePool();
            DatabaseManager.INSTANCE.runSchema();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to initialise database connection pool: {0}", e.getMessage());
            LOGGER.log(Level.SEVERE, "Aborting startup.");
            System.exit(1);
        }

        // ------------------------------------------------------------------ //
        //  4. Create and configure the HTTP server via factory                //
        // ------------------------------------------------------------------ //
        HttpServer server;
        try {
            server = HttpServerFactory.createServer(port);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to bind HTTP server on port {0}: {1}",
                    new Object[]{port, e.getMessage()});
            System.exit(1);
            return; // unreachable — satisfies compiler null-safety
        }

        // ------------------------------------------------------------------ //
        //  5. Register all route handlers                                     //
        // ------------------------------------------------------------------ //
        CorsFilter corsFilter = new CorsFilter();
        RouteRegistry.registerRoutes(server, corsFilter);

        // ------------------------------------------------------------------ //
        //  6. Register JVM shutdown hook for graceful stop                   //
        // ------------------------------------------------------------------ //
        final HttpServer serverRef = server;
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOGGER.info("Shutdown signal received — stopping server...");
            serverRef.stop(0);
            com.codeverdict.judge.JudgeQueue.getInstance().shutdown();
            DatabaseManager.INSTANCE.close();
            LOGGER.info("CodeVerdict server stopped gracefully.");
        }, "shutdown-hook"));

        // ------------------------------------------------------------------ //
        //  7. Start the server                                                //
        // ------------------------------------------------------------------ //
        server.start();
        LOGGER.log(Level.INFO, "CodeVerdict server started on port: {0}", port);
    }

    // ----------------------------------------------------------------------- //
    //  Private helpers                                                         //
    // ----------------------------------------------------------------------- //

    /**
     * Resolves the HTTP port from the {@code PORT} environment variable or
     * system property.  Falls back to {@link ServerConfig#DEFAULT_PORT} if the
     * value is absent or not a valid integer.
     *
     * @return the resolved port number
     */
    private static int resolvePort() {
        String portEnv = System.getenv("PORT");
        if (portEnv == null || portEnv.isBlank()) {
            portEnv = System.getProperty("PORT");
        }
        if (portEnv == null || portEnv.isBlank()) {
            return ServerConfig.DEFAULT_PORT;
        }
        try {
            return Integer.parseInt(portEnv.trim());
        } catch (NumberFormatException e) {
            LOGGER.log(Level.WARNING,
                    "Invalid PORT value \"{0}\" — defaulting to {1}.",
                    new Object[]{portEnv, ServerConfig.DEFAULT_PORT});
            return ServerConfig.DEFAULT_PORT;
        }
    }
}
