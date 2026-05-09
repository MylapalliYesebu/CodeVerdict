package com.codeverdict.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Abstract base class for all Data Access Objects (DAOs) in CodeVerdict.
 *
 * <p>Provides consistent, safe SQL execution helpers built on
 * {@link PreparedStatement} so that subclasses never concatenate user input
 * directly into SQL strings (preventing SQL injection).
 *
 * <p>Parameter binding uses runtime type detection to call the most specific
 * JDBC setter method.  Supported Java types and their mappings:
 * <ul>
 *   <li>{@link String}  → {@link PreparedStatement#setString}</li>
 *   <li>{@link Long}    → {@link PreparedStatement#setLong}</li>
 *   <li>{@link Integer} → {@link PreparedStatement#setInt}</li>
 *   <li>{@link Boolean} → {@link PreparedStatement#setBoolean}</li>
 *   <li>anything else   → {@link PreparedStatement#setObject}</li>
 * </ul>
 *
 * <p><strong>Connection lifecycle:</strong> each helper borrows a connection
 * from the pool and returns it after the operation.  For
 * {@link #executeQuery}, the connection is returned by the caller via
 * {@link #releaseConnection(Connection)} <em>after</em> the {@link ResultSet}
 * is fully consumed, because closing the connection before reading the
 * {@code ResultSet} would invalidate it.
 */
public abstract class BaseDao {

    /** Logger named after the concrete subclass for clear log attribution. */
    protected final Logger logger = Logger.getLogger(getClass().getName());

    /** The singleton pool manager injected at construction time. */
    protected final DatabaseManager db;

    /**
     * Constructs a DAO with the provided {@link DatabaseManager}.
     *
     * @param db the connection pool manager; must not be {@code null}
     */
    protected BaseDao(DatabaseManager db) {
        this.db = db;
    }

    // ------------------------------------------------------------------ //
    //  Protected SQL helpers                                               //
    // ------------------------------------------------------------------ //

    /**
     * Executes a {@code SELECT} query and returns the raw {@link ResultSet}.
     *
     * <p><strong>Important:</strong> the {@link Connection} associated with
     * the returned {@code ResultSet} is <em>not</em> returned to the pool
     * automatically.  The caller must:
     * <ol>
     *   <li>Consume the {@code ResultSet}.</li>
     *   <li>Close the {@code ResultSet} (or use try-with-resources).</li>
     *   <li>Release the connection via
     *       {@link #releaseConnection(Connection)}.</li>
     * </ol>
     *
     * @param sql    parameterised SQL query (use {@code ?} placeholders)
     * @param params bind parameters in positional order
     * @return the query result set
     * @throws SQLException if the query fails
     */
    protected ResultSet executeQuery(String sql, Object... params) throws SQLException {
        logger.fine(() -> "executeQuery: " + sql);
        Connection conn = db.getConnection();
        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            bindParams(ps, params);
            return ps.executeQuery();
            // conn is intentionally NOT released here; caller must call releaseConnection()
        } catch (SQLException e) {
            // Release connection on failure so it doesn't leak
            db.releaseConnection(conn);
            logger.log(Level.SEVERE, "Query failed: " + sql, e);
            throw e;
        }
    }

    /**
     * Executes a DML statement ({@code INSERT}, {@code UPDATE}, {@code DELETE})
     * and returns the number of rows affected.
     *
     * <p>The connection is borrowed and returned automatically.
     *
     * @param sql    parameterised DML statement
     * @param params bind parameters in positional order
     * @return number of rows affected
     * @throws SQLException if the statement fails
     */
    protected int executeUpdate(String sql, Object... params) throws SQLException {
        logger.fine(() -> "executeUpdate: " + sql);
        Connection conn = db.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            bindParams(ps, params);
            return ps.executeUpdate();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Update failed: " + sql, e);
            throw e;
        } finally {
            db.releaseConnection(conn);
        }
    }

    /**
     * Executes an {@code INSERT} statement and returns the generated primary key.
     *
     * <p>The connection is borrowed and returned automatically.
     *
     * @param sql    parameterised {@code INSERT} statement
     * @param params bind parameters in positional order
     * @return the auto-generated key (e.g. {@code BIGSERIAL} value)
     * @throws SQLException if the insert fails or no generated key is returned
     */
    protected long executeInsert(String sql, Object... params) throws SQLException {
        logger.fine(() -> "executeInsert: " + sql);
        Connection conn = db.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bindParams(ps, params);
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
                throw new SQLException("INSERT succeeded but no generated key was returned.");
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Insert failed: " + sql, e);
            throw e;
        } finally {
            db.releaseConnection(conn);
        }
    }

    /**
     * Returns a connection to the pool after the caller has finished consuming
     * a {@link ResultSet} obtained from {@link #executeQuery}.
     *
     * @param conn the connection to return; {@code null} is silently ignored
     */
    protected void releaseConnection(Connection conn) {
        db.releaseConnection(conn);
    }

    // ------------------------------------------------------------------ //
    //  Private helpers                                                     //
    // ------------------------------------------------------------------ //

    /**
     * Binds {@code params} to {@code ps} using the most specific JDBC setter
     * for each value's runtime type.
     *
     * @param ps     the prepared statement to bind into
     * @param params the values to bind (positional, 1-indexed)
     * @throws SQLException if a setter call fails
     */
    private void bindParams(PreparedStatement ps, Object... params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            Object p = params[i];
            int idx = i + 1; // JDBC is 1-indexed

            if (p instanceof String) {
                ps.setString(idx, (String) p);
            } else if (p instanceof Long) {
                ps.setLong(idx, (Long) p);
            } else if (p instanceof Integer) {
                ps.setInt(idx, (Integer) p);
            } else if (p instanceof Boolean) {
                ps.setBoolean(idx, (Boolean) p);
            } else {
                ps.setObject(idx, p); // handles null, BigDecimal, Timestamp, etc.
            }
        }
    }
}
