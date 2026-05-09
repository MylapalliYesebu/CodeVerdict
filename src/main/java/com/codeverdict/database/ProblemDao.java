package com.codeverdict.database;

import com.codeverdict.models.Problem;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ProblemDao extends BaseDao {

    public ProblemDao(DatabaseManager db) {
        super(db);
    }

    public List<Problem> getAllProblems(int page, int limit) {
        int offset = (page - 1) * limit;
        String sql = "SELECT * FROM problems ORDER BY created_at DESC LIMIT ? OFFSET ?";
        List<Problem> problems = new ArrayList<>();
        Connection conn = null;
        try (ResultSet rs = executeQuery(sql, limit, offset)) {
            conn = rs.getStatement().getConnection();
            while (rs.next()) {
                problems.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Query failed", e);
        } finally {
            if (conn != null) releaseConnection(conn);
        }
        return problems;
    }

    public Optional<Problem> getProblemById(long id) {
        String sql = "SELECT * FROM problems WHERE id = ?";
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

    public long createProblem(Problem p) {
        String sql = "INSERT INTO problems (title, description, difficulty, created_by) VALUES (?, ?, ?, ?)";
        try {
            return executeInsert(sql, p.getTitle(), p.getDescription(), p.getDifficulty(), p.getCreatedBy());
        } catch (SQLException e) {
            throw new RuntimeException("Insert failed", e);
        }
    }

    public int getProblemCount() {
        String sql = "SELECT COUNT(*) FROM problems";
        Connection conn = null;
        try (ResultSet rs = executeQuery(sql)) {
            conn = rs.getStatement().getConnection();
            if (rs.next()) {
                return rs.getInt(1);
            }
            return 0;
        } catch (SQLException e) {
            throw new RuntimeException("Query failed", e);
        } finally {
            if (conn != null) releaseConnection(conn);
        }
    }

    private Problem mapRow(ResultSet rs) throws SQLException {
        return new Problem(
            rs.getLong("id"),
            rs.getString("title"),
            rs.getString("description"),
            rs.getString("difficulty"),
            rs.getLong("created_by"),
            rs.getTimestamp("created_at").toInstant().toString()
        );
    }
}
