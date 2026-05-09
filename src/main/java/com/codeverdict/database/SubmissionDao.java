package com.codeverdict.database;

import com.codeverdict.models.Submission;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SubmissionDao extends BaseDao {

    public SubmissionDao(DatabaseManager db) {
        super(db);
    }

    public long createSubmission(Submission s) {
        String sql = "INSERT INTO submissions (user_id, problem_id, source_code, language, verdict) VALUES (?, ?, ?, ?, ?)";
        try {
            return executeInsert(sql, s.getUserId(), s.getProblemId(), s.getSourceCode(), s.getLanguage(), s.getVerdict());
        } catch (SQLException e) {
            throw new RuntimeException("Insert failed", e);
        }
    }

    public Optional<Submission> getSubmissionById(long id) {
        String sql = "SELECT * FROM submissions WHERE id = ?";
        Connection conn = null;
        try (ResultSet rs = executeQuery(sql, id)) {
            conn = rs.getStatement().getConnection();
            if (rs.next()) {
                return Optional.of(mapRow(rs));
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException("Query failed", e);
        } finally {
            if (conn != null) releaseConnection(conn);
        }
    }

    public List<Submission> getSubmissionsByUserId(long userId, int page, int limit) {
        int offset = (page - 1) * limit;
        String sql = "SELECT * FROM submissions WHERE user_id = ? ORDER BY submitted_at DESC LIMIT ? OFFSET ?";
        List<Submission> list = new ArrayList<>();
        Connection conn = null;
        try (ResultSet rs = executeQuery(sql, userId, limit, offset)) {
            conn = rs.getStatement().getConnection();
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Query failed", e);
        } finally {
            if (conn != null) releaseConnection(conn);
        }
        return list;
    }

    public void updateVerdict(long submissionId, String verdict, int execTimeMs) {
        String sql = "UPDATE submissions SET verdict = ?, execution_time_ms = ? WHERE id = ?";
        try {
            executeUpdate(sql, verdict, execTimeMs, submissionId);
        } catch (SQLException e) {
            throw new RuntimeException("Update failed", e);
        }
    }

    public static class LeaderboardEntry {
        public int rank;
        public long userId;
        public String username;
        public int acceptedCount;
    }

    public List<LeaderboardEntry> getLeaderboard(int limit) {
        String sql = "SELECT u.id, u.username, COUNT(DISTINCT s.problem_id) as accepted_count " +
                     "FROM users u JOIN submissions s ON s.user_id=u.id AND s.verdict='ACCEPTED' " +
                     "GROUP BY u.id, u.username ORDER BY accepted_count DESC LIMIT ?";
        List<LeaderboardEntry> list = new ArrayList<>();
        Connection conn = null;
        try (ResultSet rs = executeQuery(sql, limit)) {
            conn = rs.getStatement().getConnection();
            int rank = 1;
            while (rs.next()) {
                LeaderboardEntry e = new LeaderboardEntry();
                e.rank = rank++;
                e.userId = rs.getLong("id");
                e.username = rs.getString("username");
                e.acceptedCount = rs.getInt("accepted_count");
                list.add(e);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Query failed", e);
        } finally {
            if (conn != null) releaseConnection(conn);
        }
        return list;
    }

    private Submission mapRow(ResultSet rs) throws SQLException {
        return new Submission(
            rs.getLong("id"),
            rs.getLong("user_id"),
            rs.getLong("problem_id"),
            rs.getString("source_code"),
            rs.getString("language"),
            rs.getString("verdict"),
            rs.getTimestamp("submitted_at").toInstant().toString()
        );
    }
}
