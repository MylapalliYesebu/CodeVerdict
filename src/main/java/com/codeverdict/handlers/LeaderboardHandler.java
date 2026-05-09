package com.codeverdict.handlers;

import com.codeverdict.database.SubmissionDao;
import com.codeverdict.services.SubmissionService;
import com.codeverdict.utils.JsonUtil;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class LeaderboardHandler extends BaseHandler {

    private final SubmissionService submissionService;

    public LeaderboardHandler(SubmissionService submissionService) {
        this.submissionService = submissionService;
    }

    @Override
    protected void doHandle(HttpExchange ex) throws Exception {
        String method = getMethod(ex);

        if (!checkRateLimit(ex)) return;

        if (!"GET".equals(method)) {
            sendError(ex, 405, "Method not allowed");
            return;
        }

        List<SubmissionDao.LeaderboardEntry> leaderboard = submissionService.getLeaderboard(50);
        Map<String, Object> response = Map.of("leaderboard", leaderboard);
        sendResponse(ex, 200, JsonUtil.toJson(response));
    }
}
