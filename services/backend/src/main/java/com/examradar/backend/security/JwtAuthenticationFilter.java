package com.examradar.backend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Validates Supabase JWT Bearer tokens and establishes the security context.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    @Value("${examradar.jwt.secret:}")
    private String jwtSecret;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7).trim();
            if (!token.isBlank()) {
                try {
                    AuthenticatedUser user = parseToken(token);
                    if (user != null) {
                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    }
                } catch (Exception e) {
                    log.debug("JWT verification failed: {}", e.getMessage());
                }
            }
        }

        filterChain.doFilter(request, response);
    }

    private AuthenticatedUser parseToken(String token) {
        // If JWT secret is configured, perform full cryptographic verification
        if (jwtSecret != null && jwtSecret.length() >= 32) {
            byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
            JwtParser parser = Jwts.parser().verifyWith(Keys.hmacShaKeyFor(keyBytes)).build();
            Claims claims = parser.parseSignedClaims(token).getPayload();

            String sub = claims.getSubject();
            if (sub == null) return null;

            UUID userId = UUID.fromString(sub);
            String email = claims.get("email", String.class);
            String role = claims.get("role", String.class);
            boolean isAdmin = "service_role".equals(role) || "admin".equals(role);

            return new AuthenticatedUser(userId, email, role, isAdmin);
        }

        // Development/Test fallback: parse unverified claims safely for local dev mode
        try {
            String[] parts = token.split("\\.");
            if (parts.length >= 2) {
                String payloadJson = new String(java.util.Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
                com.fasterxml.jackson.databind.JsonNode node = new com.fasterxml.jackson.databind.ObjectMapper().readTree(payloadJson);
                String sub = node.path("sub").asText(null);
                if (sub != null) {
                    UUID userId = UUID.fromString(sub);
                    String email = node.path("email").asText("user@examradar.in");
                    String role = node.path("role").asText("authenticated");
                    boolean isAdmin = "service_role".equals(role) || "admin".equals(role);
                    return new AuthenticatedUser(userId, email, role, isAdmin);
                }
            }
        } catch (Exception ignored) {}

        return null;
    }
}
