package com.codeverdict.models;

/**
 * Represents a single test case associated with a {@link Problem}.
 *
 * <p>Public test cases are visible to users in the problem statement;
 * private test cases are used only by the judge for final verdict evaluation.
 */
public class TestCase {

    /** Unique auto-generated primary key. */
    private long id;

    /** Foreign key referencing the owning {@link Problem#getId()}. */
    private long problemId;

    /** Raw input data fed to the contestant's program via stdin. */
    private String inputData;

    /** Expected output that the contestant's program must produce. */
    private String expectedOutput;

    /**
     * Whether this test case is shown publicly in the problem statement.
     * {@code true} = sample case (visible); {@code false} = hidden judge case.
     */
    private boolean isPublic;

    // ------------------------------------------------------------------ //
    //  Constructors                                                        //
    // ------------------------------------------------------------------ //

    /** Default no-arg constructor required for JSON deserialisation. */
    public TestCase() {}

    /**
     * All-args constructor for building a fully populated {@code TestCase}.
     *
     * @param id             primary key
     * @param problemId      owning problem ID
     * @param inputData      stdin input
     * @param expectedOutput expected stdout output
     * @param isPublic       visibility flag
     */
    public TestCase(long id, long problemId, String inputData,
                    String expectedOutput, boolean isPublic) {
        this.id             = id;
        this.problemId      = problemId;
        this.inputData      = inputData;
        this.expectedOutput = expectedOutput;
        this.isPublic       = isPublic;
    }

    // ------------------------------------------------------------------ //
    //  Getters & Setters                                                   //
    // ------------------------------------------------------------------ //

    /** @return the test case's primary key */
    public long getId() { return id; }

    /** @param id the primary key to set */
    public void setId(long id) { this.id = id; }

    /** @return the owning problem's ID */
    public long getProblemId() { return problemId; }

    /** @param problemId the problem ID to set */
    public void setProblemId(long problemId) { this.problemId = problemId; }

    /** @return the stdin input data */
    public String getInputData() { return inputData; }

    /** @param inputData the input data to set */
    public void setInputData(String inputData) { this.inputData = inputData; }

    /** @return the expected stdout output */
    public String getExpectedOutput() { return expectedOutput; }

    /** @param expectedOutput the expected output to set */
    public void setExpectedOutput(String expectedOutput) { this.expectedOutput = expectedOutput; }

    /** @return {@code true} if this is a public sample test case */
    public boolean isPublic() { return isPublic; }

    /** @param isPublic {@code true} to mark this case as publicly visible */
    public void setPublic(boolean isPublic) { this.isPublic = isPublic; }

    // ------------------------------------------------------------------ //
    //  Object overrides                                                    //
    // ------------------------------------------------------------------ //

    @Override
    public String toString() {
        return "TestCase{id=" + id
                + ", problemId=" + problemId
                + ", isPublic=" + isPublic
                + '}';
    }
}
