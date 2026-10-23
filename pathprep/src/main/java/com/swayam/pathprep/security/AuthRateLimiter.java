package com.swayam.pathprep.security;

import com.swayam.pathprep.exception.RateLimitedException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory sliding window rate limiter per client IP for sensitive authentication endpoints:
 * /auth/google, /auth/refresh, /auth/dev-login.
 * Capped at 20 requests per 60 seconds per IP.
 */
@Slf4j
@Component
public class AuthRateLimiter {

    private static final int MAX_REQUESTS = 20;
    private static final long WINDOW_MILLIS = 60_000L; // 1 minute

    private final Map<String, Deque<Long>> ipRequestTimestamps = new ConcurrentHashMap<>();
    private final Clock clock;

    public AuthRateLimiter(Clock clock) {
        this.clock = clock;
    }

    public synchronized void checkRateLimit(HttpServletRequest request) {
        String clientIp = resolveClientIp(request);
        long now = clock.millis();
        long windowStart = now - WINDOW_MILLIS;

        Deque<Long> timestamps = ipRequestTimestamps.computeIfAbsent(clientIp, k -> new ArrayDeque<>());

        // Evict timestamps older than 1 minute
        while (!timestamps.isEmpty() && timestamps.peekFirst() <= windowStart) {
            timestamps.pollFirst();
        }

        if (timestamps.size() >= MAX_REQUESTS) {
            Long oldestInWindow = timestamps.peekFirst();
            long retryAfterMillis = (oldestInWindow != null ? oldestInWindow + WINDOW_MILLIS - now : WINDOW_MILLIS);
            long retryAfterSeconds = Math.max(1, (retryAfterMillis + 999) / 1000);
            log.warn("Rate limit exceeded for client IP {} ({} requests in window). Retry after {}s", clientIp, timestamps.size(), retryAfterSeconds);
            throw new RateLimitedException(retryAfterSeconds);
        }

        timestamps.addLast(now);
    }

    public synchronized void reset() {
        ipRequestTimestamps.clear();
    }

    private String resolveClientIp(HttpServletRequest request) {
        // Because server.forward-headers-strategy is configured, request.getRemoteAddr()
        // accurately reflects the client IP when forwarded through trusted reverse proxies.
        String remoteAddr = request.getRemoteAddr();
        return remoteAddr != null ? remoteAddr : "unknown";
    }
}
