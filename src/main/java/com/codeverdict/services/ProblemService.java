package com.codeverdict.services;

import com.codeverdict.database.ProblemDao;
import com.codeverdict.database.TestCaseDao;
import com.codeverdict.models.Problem;
import com.codeverdict.models.TestCase;

import java.util.List;
import java.util.Optional;

public class ProblemService {
    private final ProblemDao problemDao;
    private final TestCaseDao testCaseDao;

    private final com.codeverdict.utils.SimpleCache<String, List<Problem>> listCache = new com.codeverdict.utils.SimpleCache<>(50, 60);
    private final com.codeverdict.utils.SimpleCache<Long, Problem> itemCache = new com.codeverdict.utils.SimpleCache<>(100, 300);

    public ProblemService(ProblemDao problemDao, TestCaseDao testCaseDao) {
        this.problemDao = problemDao;
        this.testCaseDao = testCaseDao;
    }

    public List<Problem> getProblems(int page, int limit) {
        page = Math.max(1, page);
        limit = Math.max(1, Math.min(100, limit));
        String key = page + "-" + limit;
        int finalPage = page;
        int finalLimit = limit;
        return listCache.get(key).orElseGet(() -> {
            List<Problem> list = problemDao.getAllProblems(finalPage, finalLimit);
            listCache.put(key, list);
            return list;
        });
    }

    public int getProblemCount() {
        return problemDao.getProblemCount();
    }

    public Optional<Problem> getProblemById(long id) {
        Optional<Problem> cached = itemCache.get(id);
        if (cached.isPresent()) return cached;
        
        Optional<Problem> dbProb = problemDao.getProblemById(id);
        dbProb.ifPresent(p -> itemCache.put(id, p));
        return dbProb;
    }

    public List<TestCase> getPublicTestCases(long problemId) {
        return testCaseDao.getPublicTestCases(problemId);
    }

    public long createProblem(Problem problem, List<TestCase> testCases) {
        if (problem.getTitle() == null || problem.getTitle().trim().isEmpty()) {
            throw new com.codeverdict.utils.ValidationException("Problem title cannot be blank");
        }
        if (problem.getDescription() == null || problem.getDescription().trim().length() < 20) {
            throw new com.codeverdict.utils.ValidationException("Problem description must be at least 20 characters");
        }
        if (testCases == null || testCases.isEmpty()) {
            throw new com.codeverdict.utils.ValidationException("At least 1 test case is required");
        }

        long problemId = problemDao.createProblem(problem);
        for (TestCase tc : testCases) {
            tc.setProblemId(problemId);
            testCaseDao.createTestCase(tc);
        }
        listCache.clear();
        return problemId;
    }
}
