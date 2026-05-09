-- =============================================================================
-- CodeVerdict — PostgreSQL Schema
-- =============================================================================
-- Idempotent: safe to run multiple times (uses IF NOT EXISTS throughout).
-- All timestamps use TIMESTAMPTZ so they are stored in UTC and unambiguous
-- across time-zones.
-- =============================================================================


-- ---------------------------------------------------------------------------
-- 1. users
--    Core identity table. Passwords are stored as BCrypt hashes only.
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS users (
    id            BIGSERIAL     PRIMARY KEY,
    username      VARCHAR(50)   UNIQUE NOT NULL,
    email         VARCHAR(100)  UNIQUE NOT NULL,
    password_hash VARCHAR(255)  NOT NULL,
    role          VARCHAR(10)   NOT NULL DEFAULT 'USER'
                                CHECK (role IN ('USER', 'ADMIN')),
    created_at    TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);


-- ---------------------------------------------------------------------------
-- 2. problems
--    Coding problems authored by admin users.
--    created_by is nullable (SET NULL) so deleting an admin account does not
--    cascade-delete their problems.
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS problems (
    id          BIGSERIAL     PRIMARY KEY,
    title       VARCHAR(200)  NOT NULL,
    description TEXT          NOT NULL,
    difficulty  VARCHAR(10)   NOT NULL
                              CHECK (difficulty IN ('EASY', 'MEDIUM', 'HARD')),
    created_by  BIGINT        REFERENCES users(id) ON DELETE SET NULL,
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);


-- ---------------------------------------------------------------------------
-- 3. test_cases
--    Input/output pairs for a problem.  Deleted automatically when the
--    parent problem is deleted (CASCADE).
--    is_public = TRUE  → shown as sample test in the problem statement.
--    is_public = FALSE → hidden judge test case.
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS test_cases (
    id              BIGSERIAL   PRIMARY KEY,
    problem_id      BIGINT      NOT NULL REFERENCES problems(id) ON DELETE CASCADE,
    input_data      TEXT        NOT NULL,
    expected_output TEXT        NOT NULL,
    is_public       BOOLEAN     NOT NULL DEFAULT FALSE
);


-- ---------------------------------------------------------------------------
-- 4. submissions
--    Every code submission made by a user.
--    execution_time_ms is nullable — populated by the judge after evaluation.
--    verdict follows a closed set enforced by CHECK.
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS submissions (
    id                  BIGSERIAL    PRIMARY KEY,
    user_id             BIGINT       NOT NULL REFERENCES users(id)    ON DELETE CASCADE,
    problem_id          BIGINT       NOT NULL REFERENCES problems(id) ON DELETE CASCADE,
    source_code         TEXT         NOT NULL,
    language            VARCHAR(20)  NOT NULL DEFAULT 'JAVA',
    verdict             VARCHAR(30)  NOT NULL DEFAULT 'PENDING'
                                     CHECK (verdict IN (
                                         'PENDING',
                                         'ACCEPTED',
                                         'WRONG_ANSWER',
                                         'COMPILATION_ERROR',
                                         'RUNTIME_ERROR',
                                         'TIME_LIMIT_EXCEEDED'
                                     )),
    execution_time_ms   INTEGER,                          -- NULL until judge completes
    submitted_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);


-- ---------------------------------------------------------------------------
-- 5. sessions
--    Server-side session tokens issued on successful login.
--    Expired rows should be pruned periodically (e.g. pg_cron or a sweep job).
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sessions (
    id         BIGSERIAL    PRIMARY KEY,
    user_id    BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token      VARCHAR(255) UNIQUE NOT NULL,
    expires_at TIMESTAMPTZ  NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);


-- =============================================================================
-- Indexes
-- =============================================================================

-- submissions — frequently filtered by user and problem
CREATE INDEX IF NOT EXISTS idx_submissions_user_id    ON submissions(user_id);
CREATE INDEX IF NOT EXISTS idx_submissions_problem_id ON submissions(problem_id);

-- sessions — looked up by token on every authenticated request
CREATE INDEX IF NOT EXISTS idx_sessions_token         ON sessions(token);

-- sessions — supports efficient expired-session cleanup queries
CREATE INDEX IF NOT EXISTS idx_sessions_expires_at    ON sessions(expires_at);

-- test_cases — fetched in bulk by problem
CREATE INDEX IF NOT EXISTS idx_test_cases_problem_id  ON test_cases(problem_id);


-- =============================================================================
-- Verification
-- Confirms all 5 tables exist in the public schema after this script runs.
-- =============================================================================
SELECT table_name
FROM   information_schema.tables
WHERE  table_schema = 'public'
  AND  table_name   IN ('users', 'problems', 'test_cases', 'submissions', 'sessions')
ORDER  BY table_name;
