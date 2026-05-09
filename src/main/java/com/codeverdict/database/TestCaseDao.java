package com.codeverdict.database;

import com.codeverdict.models.TestCase;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class TestCaseDao extends BaseDao {

    public TestCaseDao(DatabaseManager db) {
        super(db);
    }

    public List<TestCase> getTestCasesByProblemId(long problemId) {
        String sql = "SELECT * FROM test_cases WHERE problem_id = ? ORDER BY id ASC";
        return fetchList(sql, problemId);
    }

    public List<TestCase> getPublicTestCases(long problemId) {
        String sql = "SELECT * FROM test_cases WHERE problem_id = ? AND is_public = true ORDER BY id ASC";
        return fetchList(sql, problemId);
    }

    public long createTestCase(TestCase tc) {
        String sql = "INSERT INTO test_cases (problem_id, input_data, expected_output, is_public) VALUES (?, ?, ?, ?)";
        try {
            return executeInsert(sql, tc.getProblemId(), tc.getInputData(), tc.getExpectedOutput(), tc.isPublic());
        } catch (SQLException e) {
            throw new RuntimeException("Insert failed", e);
        }
    }

    private List<TestCase> fetchList(String sql, Object... params) {
        List<TestCase> list = new ArrayList<>();
        Connection conn = null;
        try (ResultSet rs = executeQuery(sql, params)) {
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

    private TestCase mapRow(ResultSet rs) throws SQLException {
        return new TestCase(
            rs.getLong("id"),
            rs.getLong("problem_id"),
            rs.getString("input_data"),
            rs.getString("expected_output"),
            rs.getBoolean("is_public")
        );
    }
}
