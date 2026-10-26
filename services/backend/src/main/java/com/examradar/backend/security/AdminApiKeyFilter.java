package com.examradar.backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.UUID;

/**
 * Validates X-Admin-Api-Key header for trusted administrative endpoints and scheduled workers.
 * Uses constant-time comparison to prevent timing attacks.
 */
@Component
public class AdminApiKeyFilter extends OncePerRequestFilter {

    @Value("${examradar.security.admin-api-key:${examradar.admin.api-key:}}")
    private String adminApiKey;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String requestApiKey = request.getHeader("X-Admin-Api-Key");

        if (requestApiKey != null && !requestApiKey.isBlank() && isValidApiKey(requestApiKey)) {
            AuthenticatedUser adminUser = new AuthenticatedUser(
                    UUID.fromString("00000000-0000-0000-0000-000000000001"),
                    "admin@examradar.in",
                    "admin",
                    true
            );
            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(adminUser, null, adminUser.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(auth);
        }

        filterChain.doFilter(request, response);
    }

    private boolean isValidApiKey(String candidate) {
        if (adminApiKey == null || adminApiKey.isBlank()) {
            return false;
        }
        byte[] candidateBytes = candidate.getBytes(StandardCharsets.UTF_8);
        byte[] actualBytes = adminApiKey.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(candidateBytes, actualBytes);
    }
}
