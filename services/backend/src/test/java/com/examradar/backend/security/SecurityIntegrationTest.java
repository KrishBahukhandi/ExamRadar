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

    @Test
    void testForgedUnsignedJwtIsRejected() throws Exception {
        UUID testUserId = UUID.randomUUID();
        String forgedJwt = "eyJhbGciOiJub25lIn0." +
                java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(
                        ("{\"sub\":\"" + testUserId + "\",\"role\":\"admin\",\"email\":\"attacker@examradar.in\"}").getBytes(java.nio.charset.StandardCharsets.UTF_8)
                ) + ".";

        mockMvc.perform(get("/api/v1/admin/sources")
                        .header("Authorization", "Bearer " + forgedJwt)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void testJwtWithInvalidSignatureIsRejected() throws Exception {
        UUID testUserId = UUID.randomUUID();
        String wrongSecret = "d3Jvbmctc2VjcmV0LWtleS1wbGFjZWhvbGRlci1taW5pbXVtLTI1Ni1iaXRzLWxlbmd0aA==";
        String forgedJwt = io.jsonwebtoken.Jwts.builder()
                .subject(testUserId.toString())
                .claim("email", "attacker@examradar.in")
                .claim("role", "admin")
                .signWith(io.jsonwebtoken.security.Keys.hmacShaKeyFor(wrongSecret.getBytes(java.nio.charset.StandardCharsets.UTF_8)))
                .compact();

        mockMvc.perform(get("/api/v1/admin/sources")
                        .header("Authorization", "Bearer " + forgedJwt)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void testActuatorHealthIsPermitted() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void testActuatorEnvIsForbiddenWithoutAdminRole() throws Exception {
        mockMvc.perform(get("/actuator/env"))
                .andExpect(status().isForbidden());
    }
}
