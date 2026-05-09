package com.codeverdict.services;

import com.codeverdict.database.UserDao;
import com.codeverdict.models.User;
import com.codeverdict.utils.NotFoundException;

import java.util.Map;

public class UserService {
    private final UserDao userDao;
    private final com.codeverdict.database.SubmissionDao submissionDao;

    // Cache user stats: TTL=120 seconds, max=500 entries
    private static final com.codeverdict.utils.SimpleCache<Long, Map<String, Object>> statsCache = new com.codeverdict.utils.SimpleCache<>(500, 120);

    public UserService(UserDao userDao, com.codeverdict.database.SubmissionDao submissionDao) {
        this.userDao = userDao;
        this.submissionDao = submissionDao;
    }

    public static void invalidateStatsCache(long userId) {
        statsCache.invalidate(userId);
    }

    public User getUserProfile(long userId) {
        User user = userDao.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        user.setPasswordHash(null); // Never return password hash
        return user;
    }

    public Map<String, Object> getUserStats(long userId) {
        return statsCache.get(userId).orElseGet(() -> {
            java.util.List<com.codeverdict.models.Submission> submissions = submissionDao.getSubmissionsByUserId(userId, 1, 1000000);
            
            long totalSubmissions = submissions.size();
            long acceptedSubmissions = submissions.stream()
                    .filter(s -> "ACCEPTED".equals(s.getVerdict()))
                    .count();
            long problemsSolved = submissions.stream()
                    .filter(s -> "ACCEPTED".equals(s.getVerdict()))
                    .map(com.codeverdict.models.Submission::getProblemId)
                    .distinct()
                    .count();

            Map<String, Object> stats = Map.of(
                    "totalSubmissions", totalSubmissions,
                    "acceptedSubmissions", acceptedSubmissions,
                    "problemsSolved", problemsSolved
            );
            statsCache.put(userId, stats);
            return stats;
        });
    }
}
