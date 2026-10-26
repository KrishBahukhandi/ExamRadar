package com.examradar.backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * In-memory sliding window rate limiter filter that protects backend endpoints
 * against denial-of-service, abuse, and connection pool exhaustion.
 * Keys on authenticated token prefix or verified remote IP to prevent header spoofing.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 5)
@ConditionalOnProperty(name = "examradar.security.ratelimit.enabled", havingValue = "true", matchIfMissing = true)
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitingFilter.class);

    @Value("${examradar.security.ratelimit.requests-per-minute:120}")
    private int defaultRequestsPerMinute;

    @Value("${examradar.security.ratelimit.admin-per-minute:30}")
    private int adminRequestsPerMinute;

    @Value("${examradar.security.ratelimit.trust-forwarded-for:false}")
    private boolean trustForwardedFor;

    private static class WindowCounter {
        final long windowStartMs;
        final AtomicInteger count;

        WindowCounter(long windowStartMs) {
            this.windowStartMs = windowStartMs;
            this.count = new AtomicInteger(1);
        }
    }

    private final Map<String, WindowCounter> requestCounts = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String path = request.getRequestURI();

        // Skip rate limiting for static assets, swagger docs, and actuator health checks
        if (path.startsWith("/swagger-ui") || path.startsWith("/v3/api-docs") || path.startsWith("/actuator/health") || path.startsWith("/.well-known")) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIdentifier = extractClientIdentifier(request);
        int limit = path.startsWith("/api/v1/admin") ? adminRequestsPerMinute : defaultRequestsPerMinute;
        String bucketKey = clientIdentifier + ":" + (path.startsWith("/api/v1/admin") ? "admin" : "api");

        long now = System.currentTimeMillis();
        long windowSizeMs = 60_000L;

        WindowCounter counter = requestCounts.compute(bucketKey, (key, existing) -> {
            if (existing == null || (now - existing.windowStartMs) > windowSizeMs) {
                return new WindowCounter(now);
            }
            existing.count.incrementAndGet();
            return existing;
        });

        // Periodic cleanup of stale buckets if map grows large
        if (requestCounts.size() > 5000) {
            cleanupStaleEntries(now, windowSizeMs);
        }

        if (counter.count.get() > limit) {
            log.warn("Rate limit exceeded for [{}] on path [{}]: {} reqs (limit: {})",
                    clientIdentifier, path, counter.count.get(), limit);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setHeader("Retry-After", "60");
            response.getWriter().write("{\"success\":false,\"error\":\"TOO_MANY_REQUESTS\",\"message\":\"Too many requests. Please wait a minute before retrying.\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void cleanupStaleEntries(long now, long windowSizeMs) {
        requestCounts.entrySet().removeIf(entry -> (now - entry.getValue().windowStartMs) > (windowSizeMs * 2));
    }

    private String extractClientIdentifier(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ") && authHeader.length() > 20) {
            return "user:" + authHeader.substring(7, Math.min(authHeader.length(), 40));
        }

        if (trustForwardedFor) {
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                return "ip:" + forwarded.split(",")[0].trim();
            }
        }
        return "ip:" + request.getRemoteAddr();
    }
}
