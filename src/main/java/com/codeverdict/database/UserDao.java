package com.codeverdict.database;

import com.codeverdict.models.User;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

/**
 * Data Access Object for User entities.
 */
public class UserDao extends BaseDao {

    public UserDao(DatabaseManager db) {
        super(db);
    }

    /**
     * Inserts a new user into the database.
     *
     * @param user the user to create
     * @return the generated user ID
     */
    public long createUser(User user) {
        String sql = "INSERT INTO users (username, email, password_hash, role) VALUES (?, ?, ?, ?)";
        try {
            return executeInsert(sql, user.getUsername(), user.getEmail(), user.getPasswordHash(), user.getRole());
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create user", e);
        }
    }

    /**
     * Finds a user by their email address.
     *
     * @param email the email address to search for
     * @return an Optional containing the User if found, empty otherwise
     */
    public Optional<User> findByEmail(String email) {
        return findSingle("SELECT * FROM users WHERE email = ?", email);
    }

    /**
     * Finds a user by their username.
     *
     * @param username the username to search for
     * @return an Optional containing the User if found, empty otherwise
     */
    public Optional<User> findByUsername(String username) {
        return findSingle("SELECT * FROM users WHERE username = ?", username);
    }

    /**
     * Finds a user by their ID.
     *
     * @param id the user ID to search for
     * @return an Optional containing the User if found, empty otherwise
     */
    public Optional<User> findById(long id) {
        return findSingle("SELECT * FROM users WHERE id = ?", id);
    }

    /**
     * Helper method to execute a query that returns at most one User.
     */
    private Optional<User> findSingle(String sql, Object... params) {
        Connection conn = null;
        try (ResultSet rs = executeQuery(sql, params)) {
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
     * Maps a ResultSet row to a User object.
     */
    private User mapRow(ResultSet rs) throws SQLException {
        return new User(
            rs.getLong("id"),
            rs.getString("username"),
            rs.getString("email"),
            rs.getString("password_hash"),
            rs.getString("role"),
            rs.getTimestamp("created_at").toInstant().toString()
        );
    }
}
