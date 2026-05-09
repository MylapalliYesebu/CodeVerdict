package com.codeverdict.judge;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class CodeExecutor {
    private static final Logger LOGGER = Logger.getLogger(CodeExecutor.class.getName());

    public ExecutionResult execute(String submissionId, String className, String inputData) {
        Path dirPath = Paths.get(JudgeConfig.WORK_DIR, submissionId);
        Process process = null;
        try {
            ProcessBuilder pb = new ProcessBuilder("java", "-cp", ".", "-Xmx128m", "-Xss512k", className);
            pb.directory(dirPath.toFile());
            
            long startTime = System.currentTimeMillis();
            process = pb.start();

            if (inputData != null) {
                try (OutputStream os = process.getOutputStream()) {
                    os.write(inputData.getBytes(StandardCharsets.UTF_8));
                }
            }

            OutputReader stdoutReader = new OutputReader(process.getInputStream(), "stdout-reader-" + submissionId);
            OutputReader stderrReader = new OutputReader(process.getErrorStream(), "stderr-reader-" + submissionId);
            
            stdoutReader.start();
            stderrReader.start();

            boolean finished = process.waitFor(JudgeConfig.EXECUTION_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            long timeTaken = System.currentTimeMillis() - startTime;

            if (!finished) {
                process.destroyForcibly();
                return ExecutionResult.timeout(timeTaken);
            }

            stdoutReader.join(1000);
            stderrReader.join(1000);

            String stdout = truncate(stdoutReader.getOutput());
            String stderr = truncate(stderrReader.getOutput());

            if (process.exitValue() != 0) {
                return ExecutionResult.failure(stderr, timeTaken);
            }

            return ExecutionResult.success(stdout, timeTaken);

        } catch (Exception e) {
            return ExecutionResult.failure("Execution error: " + e.getMessage(), 0);
        } finally {
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
                try {
                    if (!process.waitFor(1, TimeUnit.SECONDS)) {
                        LOGGER.warning("Process didn't terminate after destroyForcibly for submission " + submissionId);
                    }
                } catch (InterruptedException ignored) {}
            }
        }
    }

    private String truncate(String text) {
        if (text == null) return "";
        if (text.length() > JudgeConfig.MAX_OUTPUT_LENGTH) {
            return text.substring(0, JudgeConfig.MAX_OUTPUT_LENGTH) + "... [output truncated]";
        }
        return text;
    }

    private static class OutputReader extends Thread {
        private final InputStream is;
        private final StringBuilder output = new StringBuilder();

        OutputReader(InputStream is, String name) {
            super(name);
            this.is = is;
        }

        @Override
        public void run() {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            } catch (IOException e) {
                // stream closed
            }
        }

        public String getOutput() {
            if (output.length() > 0 && output.charAt(output.length() - 1) == '\n') {
                return output.substring(0, output.length() - 1);
            }
            return output.toString();
        }
    }
}
