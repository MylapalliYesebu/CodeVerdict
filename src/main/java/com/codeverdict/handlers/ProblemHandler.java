package com.codeverdict.handlers;

import com.codeverdict.auth.AuthService;
import com.codeverdict.models.Problem;
import com.codeverdict.models.TestCase;
import com.codeverdict.models.User;
import com.codeverdict.services.ProblemService;
import com.codeverdict.utils.JsonUtil;
import com.codeverdict.utils.ValidationUtil;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class ProblemHandler extends BaseHandler {

    private final ProblemService problemService;
    private final AuthService authService;

    public ProblemHandler(ProblemService problemService, AuthService authService) {
        this.problemService = problemService;
        this.authService = authService;
    }

    @Override
    protected void doHandle(HttpExchange ex) throws Exception {
        String method = getMethod(ex);

        if (!checkRateLimit(ex)) return;

        if ("GET".equals(method)) {
            String param = getPathParam(ex, "/api/problems");
            if (param == null) {
                handleListProblems(ex);
            } else {
                handleGetProblem(ex, param);
            }
        } else if ("POST".equals(method)) {
            if (!validateContentType(ex)) return;
            handleCreateProblem(ex);
        } else {
            sendError(ex, 405, "Method not allowed");
        }
    }

    private void handleListProblems(HttpExchange ex) throws IOException {
        int page = 1;
        int limit = 10;
        String query = ex.getRequestURI().getQuery();
        if (query != null) {
            String[] pairs = query.split("&");
            for (String pair : pairs) {
                String[] kv = pair.split("=");
                if (kv.length == 2) {
                    try {
                        if ("page".equals(kv[0])) page = Integer.parseInt(kv[1]);
                        if ("limit".equals(kv[0])) limit = Integer.parseInt(kv[1]);
                    } catch (NumberFormatException ignored) {}
                }
            }
        }

        List<Problem> problems = problemService.getProblems(page, limit);
        int total = problemService.getProblemCount();

        Map<String, Object> response = Map.of(
            "problems", problems,
            "total", total,
            "page", page,
            "limit", limit
        );

        sendResponse(ex, 200, JsonUtil.toJson(response));
    }

    private void handleGetProblem(HttpExchange ex, String idParam) throws IOException {
        long id;
        try {
            id = Long.parseLong(idParam);
        } catch (NumberFormatException e) {
            sendError(ex, 400, "Invalid problem ID");
            return;
        }

        Optional<Problem> problemOpt = problemService.getProblemById(id);
        if (problemOpt.isEmpty()) {
            sendError(ex, 404, "Problem not found");
            return;
        }

        Problem p = problemOpt.get();
        List<TestCase> testCases = problemService.getPublicTestCases(id);

        List<Map<String, String>> tcFormat = testCases.stream().map(tc -> Map.of(
                "input", tc.getInputData(),
                "expectedOutput", tc.getExpectedOutput()
        )).collect(Collectors.toList());

        Map<String, Object> response = Map.of(
            "id", p.getId(),
            "title", p.getTitle(),
            "difficulty", p.getDifficulty(),
            "description", p.getDescription(),
            "testCases", tcFormat
        );

        sendResponse(ex, 200, JsonUtil.toJson(response));
    }

    private static class CreateProblemReq {
        String title;
        String description;
        String difficulty;
        List<TestCaseReq> testCases;
    }

    private static class TestCaseReq {
        String input;
        String expectedOutput;
        boolean isPublic;
    }

    private void handleCreateProblem(HttpExchange ex) throws IOException {
        User user = getAuthenticatedUser(ex);
        if (user == null || !"ADMIN".equals(user.getRole())) {
            sendError(ex, 403, "Forbidden: Admin access required");
            return;
        }

        String body = readBody(ex);
        CreateProblemReq req;
        try {
            req = JsonUtil.fromJson(body, CreateProblemReq.class);
            if (req == null || !ValidationUtil.isNotEmpty(req.title) || 
                !ValidationUtil.isNotEmpty(req.description) || 
                !ValidationUtil.isValidDifficulty(req.difficulty)) {
                sendError(ex, 400, "Invalid or missing fields");
                return;
            }
        } catch (Exception e) {
            sendError(ex, 400, "Malformed JSON");
            return;
        }

        Problem p = new Problem();
        p.setTitle(ValidationUtil.sanitizeString(req.title));
        p.setDescription(ValidationUtil.sanitizeString(req.description));
        p.setDifficulty(req.difficulty);
        p.setCreatedBy(user.getId());

        List<TestCase> tcs = null;
        if (req.testCases != null) {
            tcs = req.testCases.stream().map(r -> {
                TestCase t = new TestCase();
                t.setInputData(ValidationUtil.sanitizeString(r.input));
                t.setExpectedOutput(ValidationUtil.sanitizeString(r.expectedOutput));
                t.setPublic(r.isPublic);
                return t;
            }).collect(Collectors.toList());
        }

        long id = problemService.createProblem(p, tcs);
        sendResponse(ex, 201, "{\"message\":\"Problem created\",\"id\":" + id + "}");
    }

    private User getAuthenticatedUser(HttpExchange ex) {
        String authHeader = ex.getRequestHeaders().getFirst("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) return null;
        String token = authHeader.substring(7).trim();
        return authService.validateToken(token).orElse(null);
    }
}
