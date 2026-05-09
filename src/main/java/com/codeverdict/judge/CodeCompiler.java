package com.codeverdict.judge;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class CodeCompiler {

    private static final Pattern PUBLIC_CLASS_PATTERN = Pattern.compile("public\\s+class\\s+(\\w+)");

    public ExecutionResult compile(String submissionId, String sourceCode) {
        Matcher matcher = PUBLIC_CLASS_PATTERN.matcher(sourceCode);
        if (!matcher.find()) {
            return ExecutionResult.failure("No public class found in source code");
        }
        String className = matcher.group(1);

        Path dirPath = Paths.get(JudgeConfig.WORK_DIR, submissionId);
        Process process = null;
        try {
            Files.createDirectories(dirPath);
            Path sourceFile = dirPath.resolve(className + ".java");
            Files.writeString(sourceFile, sourceCode);

            ProcessBuilder pb = new ProcessBuilder("javac", "-cp", ".", className + ".java");
            pb.directory(dirPath.toFile());
            pb.redirectErrorStream(true);
            
            long startTime = System.currentTimeMillis();
            process = pb.start();
            
            boolean finished = process.waitFor(JudgeConfig.COMPILE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            long timeTaken = System.currentTimeMillis() - startTime;

            if (!finished) {
                process.destroyForcibly();
                return ExecutionResult.timeout(timeTaken);
            }

            String output;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                output = reader.lines().collect(Collectors.joining("\n"));
            }

            if (process.exitValue() != 0) {
                return ExecutionResult.failure(output, timeTaken);
            }

            return ExecutionResult.success(className, timeTaken);
            
        } catch (Exception e) {
            return ExecutionResult.failure("Compilation error: " + e.getMessage());
        } finally {
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }
        }
    }

    public void cleanup(String submissionId) {
        Path dirPath = Paths.get(JudgeConfig.WORK_DIR, submissionId);
        if (Files.exists(dirPath)) {
            try {
                Files.walk(dirPath)
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            } catch (IOException e) {
                // Ignore cleanup errors
            }
        }
    }
}
