package com.codeverdict.database;

import com.codeverdict.models.Session;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Optional;

/**
 * Data Access Object for Session entities.
 */
public class SessionDao extends BaseDao {

    public SessionDao(DatabaseManager db) {
        super(db);
    }

    /**
     * Inserts a new session into the database.
     *
     * @param userId    the authenticated user's ID
     * @param token     the generated session token
     * @param expiresAt the instant when the session expires
     */
    public void createSession(long userId, String token, Instant expiresAt) {
        String sql = "INSERT INTO sessions (user_id, token, expires_at) VALUES (?, ?, ?)";
        try {
            executeUpdate(sql, userId, token, java.sql.Timestamp.from(expiresAt));
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create session", e);
        }
    }

    /**
     * Finds a session by its token.
     *
     * @param token the session token to search for
     * @return an Optional containing the Session if found, empty otherwise
     */
    public Optional<Session> findSessionByToken(String token) {
        String sql = "SELECT * FROM sessions WHERE token = ?";
        Connection conn = null;
        try (ResultSet rs = executeQuery(sql, token)) {
            conn = rs.getStatement().getConnection();
            if (rs.next()) {
                return Optional.of(mapRow(rs));
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException("Query failed", e);
        } finally {
            if (conn != null) {
                releaseConnection(conn);
            }
        }
    }

    /**
     * Deletes a session by its token.
     *
     * @param token the session token to delete
     */
    public void deleteSession(String token) {
        String sql = "DELETE FROM sessions WHERE token = ?";
        try {
            executeUpdate(sql, token);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete session", e);
        }
    }

    /**
     * Deletes all expired sessions from the database.
     *
     * @return the number of sessions deleted
     */
    public int deleteExpiredSessions() {
        String sql = "DELETE FROM sessions WHERE expires_at < NOW()";
        try {
            return executeUpdate(sql);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete expired sessions", e);
        }
    }

    /**
     * Maps a ResultSet row to a Session object.
     */
    private Session mapRow(ResultSet rs) throws SQLException {
        return new Session(
            rs.getLong("id"),
            rs.getLong("user_id"),
            rs.getString("token"),
            rs.getTimestamp("expires_at").toInstant().toString(),
            rs.getTimestamp("created_at").toInstant().toString()
        );
    }
}
