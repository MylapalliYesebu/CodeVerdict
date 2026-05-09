package com.codeverdict.handlers;

import com.codeverdict.auth.AuthService;
import com.codeverdict.models.Submission;
import com.codeverdict.models.User;
import com.codeverdict.services.SubmissionService;
import com.codeverdict.utils.JsonUtil;
import com.codeverdict.utils.ValidationUtil;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class SubmissionHandler extends BaseHandler {

    private final SubmissionService submissionService;
    private final AuthService authService;

    public SubmissionHandler(SubmissionService submissionService, AuthService authService) {
        this.submissionService = submissionService;
        this.authService = authService;
    }

    @Override
    protected void doHandle(HttpExchange ex) throws Exception {
        String method = getMethod(ex);
        String path = ex.getRequestURI().getPath();

        if (!checkRateLimit(ex)) return;

        User user = getAuthenticatedUser(ex);
        if (user == null) {
            sendError(ex, 401, "Unauthorized");
            return;
        }

        if ("POST".equals(method) && path.equals("/api/submit")) {
            if (!validateContentType(ex)) return;
            handleSubmit(ex, user);
        } else if ("GET".equals(method) && path.startsWith("/api/submissions")) {
            String param = getPathParam(ex, "/api/submissions");
            if (param == null) {
                handleListSubmissions(ex, user);
            } else {
                handleGetSubmission(ex, user, param);
            }
        } else {
            sendError(ex, 405, "Method not allowed");
        }
    }

    private static class SubmitReq {
        Long problemId;
        String sourceCode;
        String language;
    }

    private void handleSubmit(HttpExchange ex, User user) throws IOException {
        String body = readBody(ex);
        SubmitReq req;
        try {
            req = JsonUtil.fromJson(body, SubmitReq.class);
            if (req == null || req.problemId == null || !ValidationUtil.isNotEmpty(req.sourceCode) || !ValidationUtil.isValidLanguage(req.language)) {
                sendError(ex, 400, "Invalid or missing fields");
                return;
            }
        } catch (Exception e) {
            sendError(ex, 400, "Malformed JSON");
            return;
        }

        long submissionId = submissionService.submitCode(user.getId(), req.problemId, ValidationUtil.sanitizeString(req.sourceCode), req.language);
        
        Map<String, Object> response = Map.of(
            "submissionId", submissionId,
            "status", "PENDING",
            "message", "Code submitted, judging in progress"
        );
        sendResponse(ex, 201, JsonUtil.toJson(response));
    }

    private void handleListSubmissions(HttpExchange ex, User user) throws IOException {
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

        List<Submission> submissions = submissionService.getUserSubmissions(user.getId(), page, limit);
        sendResponse(ex, 200, JsonUtil.toJson(submissions));
    }

    private void handleGetSubmission(HttpExchange ex, User user, String idParam) throws IOException {
        long id;
        try {
            id = Long.parseLong(idParam);
        } catch (NumberFormatException e) {
            sendError(ex, 400, "Invalid submission ID");
            return;
        }

        Submission sub = submissionService.getSubmission(id, user);
        sendResponse(ex, 200, JsonUtil.toJson(sub));
    }

    private User getAuthenticatedUser(HttpExchange ex) {
        String authHeader = ex.getRequestHeaders().getFirst("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) return null;
        String token = authHeader.substring(7).trim();
        return authService.validateToken(token).orElse(null);
    }
}
