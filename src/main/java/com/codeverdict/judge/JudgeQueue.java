package com.codeverdict.judge;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class JudgeQueue {
    private static final Logger LOGGER = Logger.getLogger(JudgeQueue.class.getName());
    private static JudgeQueue INSTANCE;
    
    // THREAD-SAFETY: Accessed by multiple request threads.
    // Using ExecutorService and AtomicInteger — thread-safe by design for async execution and tracking.
    private final ExecutorService executor;
    private final JudgeEngine judgeEngine;
    private final java.util.concurrent.atomic.AtomicInteger pendingJudges = new java.util.concurrent.atomic.AtomicInteger(0);

    private JudgeQueue() {
        String envPoolSize = System.getenv("JUDGE_POOL_SIZE");
        int poolSize = 3;
        if (envPoolSize != null) {
            try {
                poolSize = Integer.parseInt(envPoolSize.trim());
            } catch (NumberFormatException ignored) {}
        }
        this.executor = Executors.newFixedThreadPool(poolSize);
        this.judgeEngine = new JudgeEngine();
        LOGGER.info("JudgeQueue initialized with thread pool size " + poolSize);
    }

    public static synchronized JudgeQueue getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new JudgeQueue();
        }
        return INSTANCE;
    }

    public void submit(long submissionId) {
        LOGGER.info("Judging submission " + submissionId);
        pendingJudges.incrementAndGet();
        executor.submit(() -> {
            try {
                judgeEngine.judge(submissionId);
            } catch (Exception e) {
                LOGGER.severe("Error judging submission " + submissionId + ": " + e.getMessage());
            } finally {
                pendingJudges.decrementAndGet();
            }
        });
    }

    public int getPendingCount() {
        return pendingJudges.get();
    }

    public void shutdown() {
        LOGGER.info("Shutting down JudgeQueue...");
        executor.shutdown();
        try {
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
