package com.codeverdict.judge;

public class ExecutionResult {
    private final boolean success;
    private final String output;
    private final String errorOutput;
    private final long executionTimeMs;
    private final boolean timedOut;

    private ExecutionResult(boolean success, String output, String errorOutput, long executionTimeMs, boolean timedOut) {
        this.success = success;
        this.output = output;
        this.errorOutput = errorOutput;
        this.executionTimeMs = executionTimeMs;
        this.timedOut = timedOut;
    }

    public static ExecutionResult success(String output, long executionTimeMs) {
        return new ExecutionResult(true, output, "", executionTimeMs, false);
    }

    public static ExecutionResult success(String output) {
        return new ExecutionResult(true, output, "", 0, false);
    }

    public static ExecutionResult failure(String errorOutput, long executionTimeMs) {
        return new ExecutionResult(false, "", errorOutput, executionTimeMs, false);
    }

    public static ExecutionResult failure(String errorOutput) {
        return new ExecutionResult(false, "", errorOutput, 0, false);
    }

    public static ExecutionResult timeout(long executionTimeMs) {
        return new ExecutionResult(false, "", "Execution timed out", executionTimeMs, true);
    }

    public static ExecutionResult timeout() {
        return new ExecutionResult(false, "", "Execution timed out", 0, true);
    }

    public boolean isSuccess() { return success; }
    public String getOutput() { return output; }
    public String getErrorOutput() { return errorOutput; }
    public long getExecutionTimeMs() { return executionTimeMs; }
    public boolean isTimedOut() { return timedOut; }
}
