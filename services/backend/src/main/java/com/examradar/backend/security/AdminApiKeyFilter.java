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
import java.util.UUID;

/**
 * Validates X-Admin-Api-Key header for trusted administrative endpoints and scheduled workers.
 */
@Component
public class AdminApiKeyFilter extends OncePerRequestFilter {

    @Value("${examradar.admin.api-key:examradar-admin-secret-dev}")
    private String adminApiKey;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String requestApiKey = request.getHeader("X-Admin-Api-Key");

        if (requestApiKey != null && !requestApiKey.isBlank() && requestApiKey.equals(adminApiKey)) {
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
}
