package com.codeverdict.models;

/**
 * Represents a registered user of the CodeVerdict platform.
 */
public class User {

    /** Unique auto-generated primary key. */
    private long id;

    /** Unique display name chosen by the user. */
    private String username;

    /** Unique email address used for authentication. */
    private String email;

    /** BCrypt hash of the user's password — never stored in plain text. */
    private String passwordHash;

    /**
     * Access role governing what operations the user may perform.
     * Expected values: {@code "USER"} or {@code "ADMIN"}.
     */
    private String role;

    /** ISO-8601 timestamp of when the account was created (e.g. {@code "2024-01-15T10:30:00Z"}). */
    private String createdAt;

    // ------------------------------------------------------------------ //
    //  Constructors                                                        //
    // ------------------------------------------------------------------ //

    /** Default no-arg constructor required for JSON deserialisation. */
    public User() {}

    /**
     * All-args constructor for building a fully populated {@code User}.
     *
     * @param id           primary key
     * @param username     display name
     * @param email        email address
     * @param passwordHash BCrypt hash
     * @param role         access role
     * @param createdAt    creation timestamp
     */
    public User(long id, String username, String email,
                String passwordHash, String role, String createdAt) {
        this.id           = id;
        this.username     = username;
        this.email        = email;
        this.passwordHash = passwordHash;
        this.role         = role;
        this.createdAt    = createdAt;
    }

    // ------------------------------------------------------------------ //
    //  Getters & Setters                                                   //
    // ------------------------------------------------------------------ //

    /** @return the user's primary key */
    public long getId() { return id; }

    /** @param id the primary key to set */
    public void setId(long id) { this.id = id; }

    /** @return the user's display name */
    public String getUsername() { return username; }

    /** @param username the display name to set */
    public void setUsername(String username) { this.username = username; }

    /** @return the user's email address */
    public String getEmail() { return email; }

    /** @param email the email address to set */
    public void setEmail(String email) { this.email = email; }

    /** @return the BCrypt password hash */
    public String getPasswordHash() { return passwordHash; }

    /** @param passwordHash the BCrypt hash to set */
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    /** @return the user's role ({@code "USER"} or {@code "ADMIN"}) */
    public String getRole() { return role; }

    /** @param role the role to set */
    public void setRole(String role) { this.role = role; }

    /** @return the ISO-8601 creation timestamp */
    public String getCreatedAt() { return createdAt; }

    /** @param createdAt the creation timestamp to set */
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    // ------------------------------------------------------------------ //
    //  Object overrides                                                    //
    // ------------------------------------------------------------------ //

    @Override
    public String toString() {
        return "User{id=" + id
                + ", username='" + username + '\''
                + ", email='" + email + '\''
                + ", role='" + role + '\''
                + ", createdAt='" + createdAt + '\''
                + '}';
    }
}
