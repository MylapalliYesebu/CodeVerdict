package com.codeverdict.judge;

import com.codeverdict.database.DatabaseManager;
import com.codeverdict.database.SubmissionDao;
import com.codeverdict.database.TestCaseDao;
import com.codeverdict.models.Submission;
import com.codeverdict.models.TestCase;

import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

public class JudgeEngine {
    private static final Logger LOGGER = Logger.getLogger(JudgeEngine.class.getName());
    
    private final SubmissionDao submissionDao;
    private final TestCaseDao testCaseDao;
    private final CodeCompiler compiler;
    private final CodeExecutor executor;

    public JudgeEngine() {
        this.submissionDao = new SubmissionDao(DatabaseManager.INSTANCE);
        this.testCaseDao = new TestCaseDao(DatabaseManager.INSTANCE);
        this.compiler = new CodeCompiler();
        this.executor = new CodeExecutor();
    }

    public void judge(long submissionId) {
        Optional<Submission> opt = submissionDao.getSubmissionById(submissionId);
        if (opt.isEmpty()) {
            LOGGER.warning("Submission " + submissionId + " not found at judge time");
            return;
        }
        Submission sub = opt.get();
        String strSubId = String.valueOf(submissionId);

        List<TestCase> testCases = testCaseDao.getTestCasesByProblemId(sub.getProblemId());
        if (testCases == null || testCases.isEmpty()) {
            LOGGER.warning("No test cases found for problem " + sub.getProblemId());
            submissionDao.updateVerdict(submissionId, "ACCEPTED", 0);
            return;
        }

        try {
            ExecutionResult compileResult = compiler.compile(strSubId, sub.getSourceCode());
            if (!compileResult.isSuccess()) {
                submissionDao.updateVerdict(submissionId, "COMPILATION_ERROR", 0);
                return;
            }
            
            String className = compileResult.getOutput();
            if (className == null || className.isEmpty()) {
                submissionDao.updateVerdict(submissionId, "COMPILATION_ERROR", 0);
                return;
            }

            String finalVerdict = "ACCEPTED";
            long totalExecutionTimeMs = 0;

            for (TestCase tc : testCases) {
                String input = tc.getInputData() == null ? "" : tc.getInputData();
                ExecutionResult execResult = executor.execute(strSubId, className, input);
                
                totalExecutionTimeMs += execResult.getExecutionTimeMs();

                if (execResult.isTimedOut()) {
                    finalVerdict = "TIME_LIMIT_EXCEEDED";
                    break;
                }
                
                if (!execResult.isSuccess()) {
                    finalVerdict = "RUNTIME_ERROR";
                    break;
                }

                String verdict = VerdictCalculator.calculateVerdict(execResult.getOutput(), tc.getExpectedOutput());
                if ("WRONG_ANSWER".equals(verdict)) {
                    finalVerdict = "WRONG_ANSWER";
                    break;
                }
            }

            submissionDao.updateVerdict(submissionId, finalVerdict, (int) totalExecutionTimeMs);
            com.codeverdict.services.UserService.invalidateStatsCache(sub.getUserId());

        } finally {
            compiler.cleanup(strSubId);
        }
    }
}
