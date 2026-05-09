package com.codeverdict.utils;

import com.sun.net.httpserver.HttpExchange;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * IP-based token bucket rate limiter to prevent abuse.
 * 
 * THREAD-SAFETY: Accessed by multiple request threads.
 * Using ConcurrentHashMap.compute() — ensures atomic token updates per IP without explicit locking.
 */
public class RateLimiter {

    private static final Logger LOGGER = Logger.getLogger(RateLimiter.class.getName());
    
    private static final int DEFAULT_RATE_LIMIT = 60;
    private final int maxTokens;
    private final ConcurrentHashMap<String, RateLimitState> buckets = new ConcurrentHashMap<>();

    private static final RateLimiter INSTANCE = new RateLimiter();

    private RateLimiter() {
        String envLimit = System.getenv("RATE_LIMIT_PER_MINUTE");
        if (envLimit == null) {
            envLimit = System.getProperty("RATE_LIMIT_PER_MINUTE");
        }
        int limit = DEFAULT_RATE_LIMIT;
        if (envLimit != null) {
            try {
                limit = Integer.parseInt(envLimit.trim());
            } catch (NumberFormatException e) {
                LOGGER.warning("Invalid RATE_LIMIT_PER_MINUTE, defaulting to " + DEFAULT_RATE_LIMIT);
            }
        }
        this.maxTokens = limit;

        // Schedule cleanup to prevent memory leaks from inactive IPs
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "RateLimiter-Cleanup");
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleAtFixedRate(this::cleanup, 5, 5, TimeUnit.MINUTES);
    }

    public static RateLimiter getInstance() {
        return INSTANCE;
    }

    /**
     * Attempts to acquire a token for the given IP address.
     * Returns true if successful, false if the rate limit is exceeded.
     */
    public boolean tryAcquire(String ipAddress) {
        Instant now = Instant.now();
        boolean[] allowed = {false};
        
        buckets.compute(ipAddress, (key, state) -> {
            if (state == null) {
                allowed[0] = true;
                return new RateLimitState(maxTokens - 1, now);
            }
            
            long secondsPassed = ChronoUnit.SECONDS.between(state.lastRefill, now);
            if (secondsPassed >= 60) {
                allowed[0] = true;
                return new RateLimitState(maxTokens - 1, now);
            }
            
            if (state.tokens > 0) {
                state.tokens--;
                allowed[0] = true;
            }
            return state;
        });
        
        if (!allowed[0]) {
            LOGGER.warning("Rate limit exceeded for IP: " + ipAddress);
        }
        
        return allowed[0];
    }

    /**
     * Extracts the client's actual IP address, respecting X-Forwarded-For if running behind a proxy.
     */
    public String getClientIp(HttpExchange ex) {
        String xForwardedFor = ex.getRequestHeaders().getFirst("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            String[] ips = xForwardedFor.split(",");
            return ips[0].trim();
        }
        return ex.getRemoteAddress().getAddress().getHostAddress();
    }

    /**
     * Periodically removes IPs that haven't made a request in the last 5 minutes.
     */
    private void cleanup() {
        Instant threshold = Instant.now().minus(5, ChronoUnit.MINUTES);
        Iterator<Map.Entry<String, RateLimitState>> iterator = buckets.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, RateLimitState> entry = iterator.next();
            if (entry.getValue().lastRefill.isBefore(threshold)) {
                iterator.remove();
            }
        }
    }

    private static class RateLimitState {
        int tokens;
        Instant lastRefill;

        RateLimitState(int tokens, Instant lastRefill) {
            this.tokens = tokens;
            this.lastRefill = lastRefill;
        }
    }
}
