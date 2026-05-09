package com.codeverdict.handlers;

import com.sun.net.httpserver.HttpExchange;

import java.time.Instant;

/**
 * Handles the {@code GET /api/health} liveness endpoint.
 *
 * <p>Returns a {@code 200 OK} JSON response containing the service name and
 * the current server timestamp so that Render, load balancers, and uptime
 * monitors can verify the service is alive and measure response time.
 *
 * <p>Response body:
 * <pre>
 * {
 *   "status": "UP",
 *   "service": "CodeVerdict",
 *   "timestamp": "2024-01-15T10:30:00Z"
 * }
 * </pre>
 *
 * <p>Any HTTP method other than {@code GET} receives a {@code 405 Method Not
 * Allowed} response.
 */
public class HealthHandler extends BaseHandler {

    /** Service name included in the health response payload. */
    private static final String SERVICE_NAME = "CodeVerdict";

    @Override
    protected void doHandle(HttpExchange ex) throws Exception {
        if (!"GET".equals(getMethod(ex))) {
            sendError(ex, 405, "Method not allowed");
            return;
        }

        String timestamp = Instant.now().toString(); // ISO-8601, e.g. 2024-01-15T10:30:00Z
        int dbPool = com.codeverdict.database.DatabaseManager.INSTANCE.getAvailableConnections();
        int pending = com.codeverdict.judge.JudgeQueue.getInstance().getPendingCount();
        long uptime = java.lang.management.ManagementFactory.getRuntimeMXBean().getUptime() / 1000;

        String body = String.format(
                "{\"status\":\"UP\",\"service\":\"%s\",\"timestamp\":\"%s\",\"dbPoolAvailable\":%d,\"judgeQueuePending\":%d,\"uptimeSeconds\":%d}",
                SERVICE_NAME, timestamp, dbPool, pending, uptime);

        sendResponse(ex, 200, body);
    }
}
