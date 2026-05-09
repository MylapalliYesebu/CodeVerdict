package com.codeverdict.server;

import com.sun.net.httpserver.Filter;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.util.logging.Logger;

public class RequestLoggingFilter extends Filter {
    private static final Logger LOGGER = Logger.getLogger(RequestLoggingFilter.class.getName());

    @Override
    public String description() {
        return "Logs all incoming HTTP requests.";
    }

    @Override
    public void doFilter(HttpExchange exchange, Chain chain) throws IOException {
        long start = System.currentTimeMillis();
        try {
            chain.doFilter(exchange);
        } finally {
            long duration = System.currentTimeMillis() - start;
            int status = exchange.getResponseCode();
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();
            String ip = getClientIp(exchange);

            String logMsg = String.format("[REQUEST] %s %s %d %dms from %s", method, path, status, duration, ip);

            if (status >= 500) {
                LOGGER.severe(logMsg);
            } else if (status >= 400) {
                LOGGER.warning(logMsg);
            } else {
                LOGGER.info(logMsg);
            }
        }
    }

    private String getClientIp(HttpExchange ex) {
        String xForwardedFor = ex.getRequestHeaders().getFirst("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            String[] ips = xForwardedFor.split(",");
            return ips[0].trim();
        }
        return ex.getRemoteAddress().getAddress().getHostAddress();
    }
}
