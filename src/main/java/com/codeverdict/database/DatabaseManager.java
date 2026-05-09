package com.codeverdict.database;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Enum-based singleton that manages a JDBC connection pool for PostgreSQL.
 *
 * <p>Using an {@code enum} guarantees thread-safe, lazy-initialised singleton
 * semantics without explicit synchronisation or double-checked locking.
 *
 * <p>Typical startup sequence in {@code Main}:
 * <pre>
 *   DatabaseManager.INSTANCE.initializePool();
 *   DatabaseManager.INSTANCE.runSchema();
 * </pre>
 *
 * <p>Connection pool configuration is read from environment variables (or
 * system properties set by {@code EnvConfig}):
 * <ul>
 *   <li>{@code DB_URL}       — JDBC URL, e.g. {@code jdbc:postgresql://host:5432/dbname}</li>
 *   <li>{@code DB_USER}      — database username</li>
 *   <li>{@code DB_PASSWORD}  — database password</li>
 *   <li>{@code DB_POOL_SIZE} — pool size (default: {@value #DEFAULT_POOL_SIZE})</li>
 * </ul>
 */
public enum DatabaseManager {

    /** The single shared instance. */
    INSTANCE;

    // ------------------------------------------------------------------ //
    //  Constants                                                           //
    // ------------------------------------------------------------------ //

    private static final Logger LOGGER = Logger.getLogger(DatabaseManager.class.getName());

    /** Default connection pool size when {@code DB_POOL_SIZE} is absent or invalid. */
    private static final int DEFAULT_POOL_SIZE = 10;

    /** Seconds to wait for an available connection before giving up. */
    private static final long CONNECTION_TIMEOUT_SECONDS = 5;

    /** Classpath path to the SQL schema file. */
    private static final String SCHEMA_RESOURCE = "/schema.sql";

    // ------------------------------------------------------------------ //
    //  Pool state                                                          //
    // ------------------------------------------------------------------ //

    // THREAD-SAFETY: Accessed by multiple request threads.
    // Using enum singleton and LinkedBlockingQueue — guarantees single instance and thread-safe connection pooling.

    /** Thread-safe queue used as the connection pool. */
    private LinkedBlockingQueue<Connection> pool;

    private final java.util.concurrent.atomic.AtomicInteger activeConnections = new java.util.concurrent.atomic.AtomicInteger(0);

    /** Cached JDBC URL, validated at startup. */
    private String dbUrl;

    /** Cached DB user. */
    private String dbUser;

    /** Cached DB password. */
    private String dbPassword;

    // ------------------------------------------------------------------ //
    //  Lifecycle                                                           //
    // ------------------------------------------------------------------ //

    /**
     * Reads database credentials from the environment, validates them, and
     * pre-creates {@code poolSize} connections.
     *
     * @throws IllegalStateException if {@code DB_URL} is missing
     * @throws RuntimeException      if any connection cannot be established
     */
    public void initializePool() {
        dbUrl      = resolveEnv("DB_URL");
        dbUser     = resolveEnv("DB_USER");
        dbPassword = resolveEnv("DB_PASSWORD");

        if (dbUrl == null || dbUrl.isBlank()) {
            throw new IllegalStateException(
                    "DB_URL environment variable is not set. "
                    + "Expected format: jdbc:postgresql://host:port/dbname");
        }

        int poolSize = resolvePoolSize();
        pool = new LinkedBlockingQueue<>(poolSize);

        LOGGER.log(Level.INFO, "Initialising DB connection pool (size={0}) for: {1}",
                new Object[]{poolSize, dbUrl});

        for (int i = 0; i < poolSize; i++) {
            pool.offer(createConnection());
        }

        LOGGER.info("DB connection pool ready.");
    }

    /**
     * Borrows a connection from the pool, blocking up to
     * {@value #CONNECTION_TIMEOUT_SECONDS} seconds.
     *
     * @return a live {@link Connection}
     * @throws RuntimeException if no connection becomes available within the timeout
     */
    public Connection getConnection() {
        try {
            Connection conn = pool.poll(CONNECTION_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (conn == null) {
                throw new RuntimeException("Connection pool exhausted — no connection "
                        + "available after " + CONNECTION_TIMEOUT_SECONDS + "s.");
            }
            // Replace stale connection transparently
            if (conn.isClosed()) {
                LOGGER.warning("Stale connection detected — creating replacement.");
                conn = createConnection();
            }
            activeConnections.incrementAndGet();
            return conn;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while waiting for a DB connection.", e);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to check connection state.", e);
        }
    }

    /**
     * Returns a borrowed connection to the pool.
     *
     * <p>If the connection has been closed externally, a fresh replacement is
     * created and returned to the pool so the pool size stays constant.
     *
     * @param conn the connection to return; {@code null} is safely ignored
     */
    public void releaseConnection(Connection conn) {
        if (conn == null) return;
        activeConnections.decrementAndGet();
        try {
            if (conn.isClosed()) {
                LOGGER.warning("Closed connection returned to pool — creating replacement.");
                pool.offer(createConnection());
            } else {
                pool.offer(conn);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error checking connection status on release.", e);
            // Best-effort: create a replacement so pool doesn't shrink
            try {
                pool.offer(createConnection());
            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, "Failed to create replacement connection.", ex);
            }
        }
    }

    /**
     * Reads {@code schema.sql} from the classpath and executes each SQL
     * statement against the database.
     *
     * <p>Statements are split on {@code ;} so the file can contain DDL,
     * indexes, and the trailing verification {@code SELECT}.  Blank lines
     * and comment-only blocks are skipped.
     *
     * @throws RuntimeException if the schema file cannot be read or executed
     */
    public void runSchema() {
        LOGGER.info("Applying schema from classpath:" + SCHEMA_RESOURCE);

        try (InputStream is = DatabaseManager.class.getResourceAsStream(SCHEMA_RESOURCE)) {
            if (is == null) {
                throw new RuntimeException(
                        "schema.sql not found on classpath at " + SCHEMA_RESOURCE);
            }

            String fullSql;
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(is, StandardCharsets.UTF_8))) {
                fullSql = reader.lines().collect(Collectors.joining("\n"));
            }

            // Split on semicolons and execute each non-empty statement
            Connection conn = getConnection();
            try (Statement stmt = conn.createStatement()) {
                for (String rawStatement : fullSql.split(";")) {
                    String sql = stripComments(rawStatement).trim();
                    if (!sql.isBlank()) {
                        LOGGER.fine("Executing: " + sql.substring(0, Math.min(80, sql.length())));
                        stmt.execute(sql);
                    }
                }
                LOGGER.info("Schema applied successfully.");
            } finally {
                releaseConnection(conn);
            }

        } catch (IOException e) {
            throw new RuntimeException("Failed to read schema.sql from classpath.", e);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to execute schema.sql.", e);
        }
    }

    /**
     * Closes all connections in the pool.  Called from the JVM shutdown hook
     * registered in {@code Main}.
     */
    public void close() {
        if (pool == null) return;
        LOGGER.info("Closing all database connections...");
        int closed = 0;
        Connection conn;
        while ((conn = pool.poll()) != null) {
            try {
                if (!conn.isClosed()) {
                    conn.close();
                    closed++;
                }
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Error closing pooled connection.", e);
            }
        }
        LOGGER.log(Level.INFO, "Database connection pool closed ({0} connections).", closed);
    }

    /**
     * Retrieves the current number of available connections in the pool.
     */
    public int getAvailableConnections() {
        return pool == null ? 0 : pool.size();
    }

    // ------------------------------------------------------------------ //
    //  Private helpers                                                     //
    // ------------------------------------------------------------------ //

    /**
     * Opens a new physical JDBC connection using the cached credentials.
     *
     * @return a fresh, open {@link Connection}
     * @throws RuntimeException if the connection cannot be established
     */
    private Connection createConnection() {
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("PostgreSQL JDBC Driver not found", e);
        }
        try {
            return DriverManager.getConnection(dbUrl, dbUser, dbPassword);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to create database connection to {0}: {1}",
                    new Object[]{dbUrl, e.getMessage()});
            throw new RuntimeException("Unable to create database connection.", e);
        }
    }

    /**
     * Reads an environment variable, falling back to the system property of
     * the same name (populated by {@code EnvConfig.load()}).
     *
     * @param key variable name
     * @return the value, or {@code null} if absent in both sources
     */
    private String resolveEnv(String key) {
        String val = System.getenv(key);
        return (val != null) ? val : System.getProperty(key);
    }

    /**
     * Resolves the pool size from {@code DB_POOL_SIZE}, with validation and
     * fallback to {@value #DEFAULT_POOL_SIZE}.
     */
    private int resolvePoolSize() {
        String raw = resolveEnv("DB_POOL_SIZE");
        if (raw == null || raw.isBlank()) return DEFAULT_POOL_SIZE;
        try {
            int size = Integer.parseInt(raw.trim());
            if (size < 1) throw new NumberFormatException("must be >= 1");
            return size;
        } catch (NumberFormatException e) {
            LOGGER.log(Level.WARNING,
                    "Invalid DB_POOL_SIZE \"{0}\" — defaulting to {1}.",
                    new Object[]{raw, DEFAULT_POOL_SIZE});
            return DEFAULT_POOL_SIZE;
        }
    }

    /**
     * Removes SQL line comments ({@code --}) from a statement block so that
     * stripped text can be checked with {@code isBlank()}.
     */
    private String stripComments(String sql) {
        StringBuilder sb = new StringBuilder();
        for (String line : sql.split("\n")) {
            int commentIdx = line.indexOf("--");
            String stripped = (commentIdx >= 0) ? line.substring(0, commentIdx) : line;
            sb.append(stripped).append('\n');
        }
        return sb.toString();
    }
}
