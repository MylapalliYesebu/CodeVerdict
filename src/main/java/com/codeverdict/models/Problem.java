package com.codeverdict.models;

/**
 * Represents a coding problem available on the CodeVerdict platform.
 */
public class Problem {

    /** Unique auto-generated primary key. */
    private long id;

    /** Short, descriptive title displayed in the problem list. */
    private String title;

    /** Full Markdown-formatted problem statement shown to contestants. */
    private String description;

    /**
     * Difficulty rating of the problem.
     * Expected values: {@code "EASY"}, {@code "MEDIUM"}, or {@code "HARD"}.
     */
    private String difficulty;

    /** Foreign key referencing the {@link User#getId()} of the problem author. */
    private long createdBy;

    /** ISO-8601 timestamp of when the problem was created. */
    private String createdAt;

    // ------------------------------------------------------------------ //
    //  Constructors                                                        //
    // ------------------------------------------------------------------ //

    /** Default no-arg constructor required for JSON deserialisation. */
    public Problem() {}

    /**
     * All-args constructor for building a fully populated {@code Problem}.
     *
     * @param id          primary key
     * @param title       problem title
     * @param description full problem statement
     * @param difficulty  difficulty level
     * @param createdBy   author user ID
     * @param createdAt   creation timestamp
     */
    public Problem(long id, String title, String description,
                   String difficulty, long createdBy, String createdAt) {
        this.id          = id;
        this.title       = title;
        this.description = description;
        this.difficulty  = difficulty;
        this.createdBy   = createdBy;
        this.createdAt   = createdAt;
    }

    // ------------------------------------------------------------------ //
    //  Getters & Setters                                                   //
    // ------------------------------------------------------------------ //

    /** @return the problem's primary key */
    public long getId() { return id; }

    /** @param id the primary key to set */
    public void setId(long id) { this.id = id; }

    /** @return the problem title */
    public String getTitle() { return title; }

    /** @param title the title to set */
    public void setTitle(String title) { this.title = title; }

    /** @return the full problem description */
    public String getDescription() { return description; }

    /** @param description the description to set */
    public void setDescription(String description) { this.description = description; }

    /** @return the difficulty level */
    public String getDifficulty() { return difficulty; }

    /** @param difficulty the difficulty to set */
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }

    /** @return the author's user ID */
    public long getCreatedBy() { return createdBy; }

    /** @param createdBy the author user ID to set */
    public void setCreatedBy(long createdBy) { this.createdBy = createdBy; }

    /** @return the ISO-8601 creation timestamp */
    public String getCreatedAt() { return createdAt; }

    /** @param createdAt the creation timestamp to set */
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    // ------------------------------------------------------------------ //
    //  Object overrides                                                    //
    // ------------------------------------------------------------------ //

    @Override
    public String toString() {
        return "Problem{id=" + id
                + ", title='" + title + '\''
                + ", difficulty='" + difficulty + '\''
                + ", createdBy=" + createdBy
                + ", createdAt='" + createdAt + '\''
                + '}';
    }
}
