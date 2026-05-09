package com.codeverdict.services;

import com.codeverdict.database.ProblemDao;
import com.codeverdict.database.SubmissionDao;
import com.codeverdict.models.Submission;
import com.codeverdict.models.User;
import com.codeverdict.utils.ForbiddenException;
import com.codeverdict.utils.NotFoundException;
import com.codeverdict.utils.SpamSubmissionException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class SubmissionService {
    private final SubmissionDao submissionDao;
    private final ProblemDao problemDao;

    // Spam prevention state
    private final ConcurrentHashMap<Long, Instant> recentSubmissions = new ConcurrentHashMap<>();

    public SubmissionService(SubmissionDao submissionDao, ProblemDao problemDao) {
        this.submissionDao = submissionDao;
        this.problemDao = problemDao;
    }

    public long submitCode(long userId, long problemId, String sourceCode, String language) {
        // THREAD-SAFETY: Accessed by multiple request threads.
        // Using ConcurrentHashMap.compute() and AtomicReference — ensures atomic spam checking without race conditions.
        java.util.concurrent.atomic.AtomicReference<Boolean> spamDetected = new java.util.concurrent.atomic.AtomicReference<>(false);
        Instant now = Instant.now();
        recentSubmissions.compute(userId, (k, lastTime) -> {
            if (lastTime != null && ChronoUnit.SECONDS.between(lastTime, now) < 10) {
                spamDetected.set(true);
            }
            return now;
        });

        if (spamDetected.get()) {
            throw new SpamSubmissionException("You are submitting too fast. Please wait 10 seconds.");
        }

        // Verify problem exists
        if (problemDao.getProblemById(problemId).isEmpty()) {
            throw new NotFoundException("Problem does not exist");
        }

        Submission s = new Submission();
        s.setUserId(userId);
        s.setProblemId(problemId);
        s.setSourceCode(sourceCode);
        s.setLanguage(language);
        s.setVerdict("PENDING");
        long submissionId = submissionDao.createSubmission(s);
        com.codeverdict.judge.JudgeQueue.getInstance().submit(submissionId);
        return submissionId;
    }

    public Submission getSubmission(long id, User requestingUser) {
        Submission sub = submissionDao.getSubmissionById(id)
                .orElseThrow(() -> new NotFoundException("Submission not found"));

        if (sub.getUserId() != requestingUser.getId() && !"ADMIN".equals(requestingUser.getRole())) {
            throw new ForbiddenException("You don't own this submission");
        }

        return sub;
    }

    public List<Submission> getUserSubmissions(long userId, int page, int limit) {
        page = Math.max(1, page);
        limit = Math.max(1, Math.min(100, limit));
        return submissionDao.getSubmissionsByUserId(userId, page, limit);
    }

    public List<SubmissionDao.LeaderboardEntry> getLeaderboard(int limit) {
        return submissionDao.getLeaderboard(limit);
    }
}
