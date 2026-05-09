package com.codeverdict.models;

/**
 * Represents a user session after successful authentication.
 */
public class Session {

    /** Unique auto-generated primary key. */
    private long id;

    /** Foreign key referencing the authenticated user's ID. */
    private long userId;

    /** The unique session token used for authentication via Bearer token. */
    private String token;

    /** ISO-8601 timestamp of when the session will expire. */
    private String expiresAt;

    /** ISO-8601 timestamp of when the session was created. */
    private String createdAt;

    // ------------------------------------------------------------------ //
    //  Constructors                                                        //
    // ------------------------------------------------------------------ //

    /** Default no-arg constructor required for JSON deserialisation. */
    public Session() {}

    /**
     * All-args constructor for building a fully populated {@code Session}.
     *
     * @param id        primary key
     * @param userId    authenticated user's ID
     * @param token     session token
     * @param expiresAt expiration timestamp
     * @param createdAt creation timestamp
     */
    public Session(long id, long userId, String token, String expiresAt, String createdAt) {
        this.id = id;
        this.userId = userId;
        this.token = token;
        this.expiresAt = expiresAt;
        this.createdAt = createdAt;
    }

    // ------------------------------------------------------------------ //
    //  Getters & Setters                                                   //
    // ------------------------------------------------------------------ //

    /** @return the session's primary key */
    public long getId() { return id; }

    /** @param id the primary key to set */
    public void setId(long id) { this.id = id; }

    /** @return the authenticated user's ID */
    public long getUserId() { return userId; }

    /** @param userId the user ID to set */
    public void setUserId(long userId) { this.userId = userId; }

    /** @return the session token */
    public String getToken() { return token; }

    /** @param token the session token to set */
    public void setToken(String token) { this.token = token; }

    /** @return the ISO-8601 expiration timestamp */
    public String getExpiresAt() { return expiresAt; }

    /** @param expiresAt the expiration timestamp to set */
    public void setExpiresAt(String expiresAt) { this.expiresAt = expiresAt; }

    /** @return the ISO-8601 creation timestamp */
    public String getCreatedAt() { return createdAt; }

    /** @param createdAt the creation timestamp to set */
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    // ------------------------------------------------------------------ //
    //  Object overrides                                                    //
    // ------------------------------------------------------------------ //

    @Override
    public String toString() {
        return "Session{" +
                "id=" + id +
                ", userId=" + userId +
                ", expiresAt='" + expiresAt + '\'' +
                ", createdAt='" + createdAt + '\'' +
                '}';
    }
}
