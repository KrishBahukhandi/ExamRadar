package com.examradar.backend.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testUnauthenticatedAccessToMeEndpointIsForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/me/preferences")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void testUnauthenticatedAccessToAdminEndpointIsForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/admin/sources")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void testAdminAccessWithValidApiKeySucceeds() throws Exception {
        mockMvc.perform(get("/api/v1/admin/sources")
                        .header("X-Admin-Api-Key", "examradar-admin-secret-dev")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void testAuthenticatedUserAccessWithBearerToken() throws Exception {
        UUID testUserId = UUID.randomUUID();
        String secret = "dGVzdC1qd3Qtc2VjcmV0LXBsYWNlaG9sZGVyLW1pbmltdW0tMjU2LWJpdHMtbGVuZ3Ro";
        String mockJwt = io.jsonwebtoken.Jwts.builder()
                .subject(testUserId.toString())
                .claim("email", "tester@examradar.in")
                .claim("role", "authenticated")
                .signWith(io.jsonwebtoken.security.Keys.hmacShaKeyFor(secret.getBytes(java.nio.charset.StandardCharsets.UTF_8)))
                .compact();

        mockMvc.perform(get("/api/v1/me/preferences")
                        .header("Authorization", "Bearer " + mockJwt)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
