package com.codeverdict.models;

/**
 * Represents a code submission made by a user for a specific problem.
 *
 * <p>A submission captures the full source code, the programming language
 * used, and the judge's final verdict after evaluating all test cases.
 */
public class Submission {

    /** Unique auto-generated primary key. */
    private long id;

    /** Foreign key referencing the submitting {@link User#getId()}. */
    private long userId;

    /** Foreign key referencing the target {@link Problem#getId()}. */
    private long problemId;

    /** Full source code submitted by the user. */
    private String sourceCode;

    /**
     * Programming language of the submission.
     * Expected values: {@code "JAVA"}, {@code "PYTHON"}, {@code "C++"}, etc.
     */
    private String language;

    /**
     * Judge verdict for this submission.
     * Expected values: {@code "ACCEPTED"}, {@code "WRONG_ANSWER"},
     * {@code "TIME_LIMIT_EXCEEDED"}, {@code "COMPILATION_ERROR"},
     * {@code "RUNTIME_ERROR"}, {@code "PENDING"}.
     */
    private String verdict;

    /** ISO-8601 timestamp of when the submission was received. */
    private String timestamp;

    // ------------------------------------------------------------------ //
    //  Constructors                                                        //
    // ------------------------------------------------------------------ //

    /** Default no-arg constructor required for JSON deserialisation. */
    public Submission() {}

    /**
     * All-args constructor for building a fully populated {@code Submission}.
     *
     * @param id         primary key
     * @param userId     submitting user's ID
     * @param problemId  target problem's ID
     * @param sourceCode submitted source code
     * @param language   programming language
     * @param verdict    judge verdict
     * @param timestamp  submission timestamp
     */
    public Submission(long id, long userId, long problemId, String sourceCode,
                      String language, String verdict, String timestamp) {
        this.id         = id;
        this.userId     = userId;
        this.problemId  = problemId;
        this.sourceCode = sourceCode;
        this.language   = language;
        this.verdict    = verdict;
        this.timestamp  = timestamp;
    }

    // ------------------------------------------------------------------ //
    //  Getters & Setters                                                   //
    // ------------------------------------------------------------------ //

    /** @return the submission's primary key */
    public long getId() { return id; }

    /** @param id the primary key to set */
    public void setId(long id) { this.id = id; }

    /** @return the submitting user's ID */
    public long getUserId() { return userId; }

    /** @param userId the user ID to set */
    public void setUserId(long userId) { this.userId = userId; }

    /** @return the target problem's ID */
    public long getProblemId() { return problemId; }

    /** @param problemId the problem ID to set */
    public void setProblemId(long problemId) { this.problemId = problemId; }

    /** @return the full source code */
    public String getSourceCode() { return sourceCode; }

    /** @param sourceCode the source code to set */
    public void setSourceCode(String sourceCode) { this.sourceCode = sourceCode; }

    /** @return the programming language identifier */
    public String getLanguage() { return language; }

    /** @param language the language to set */
    public void setLanguage(String language) { this.language = language; }

    /** @return the judge verdict */
    public String getVerdict() { return verdict; }

    /** @param verdict the verdict to set */
    public void setVerdict(String verdict) { this.verdict = verdict; }

    /** @return the ISO-8601 submission timestamp */
    public String getTimestamp() { return timestamp; }

    /** @param timestamp the timestamp to set */
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    // ------------------------------------------------------------------ //
    //  Object overrides                                                    //
    // ------------------------------------------------------------------ //

    @Override
    public String toString() {
        return "Submission{id=" + id
                + ", userId=" + userId
                + ", problemId=" + problemId
                + ", language='" + language + '\''
                + ", verdict='" + verdict + '\''
                + ", timestamp='" + timestamp + '\''
                + '}';
    }
}
