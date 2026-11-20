package com.meocare.meocare.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthAndUserSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void unauthenticatedRequestsToProtectedEndpointsReturnJson401() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.path").value("/api/auth/me"));

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void customerRegisterLoginProfileUpdateAndRbacFlow() throws Exception {
        String email = "sarah.jenkins." + System.nanoTime() + "@oakmail.com";

        // 1. Register strictly creates CUSTOMER
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "Sarah Jenkins",
                                  "email": "%s",
                                  "password": "Password123!",
                                  "phone": "555-0199"
                                }
                                """.formatted(email)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("CUSTOMER"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.password").doesNotExist());

        // 2. Login returns real JWT
        String token = loginAndGetToken(email, "Password123!");
        assertThat(token).isNotBlank();

        // 3. GET /api/auth/me succeeds
        mockMvc.perform(get("/api/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email.toLowerCase()))
                .andExpect(jsonPath("$.role").value("CUSTOMER"));

        // 4. PUT /api/auth/me updates profile and password
        mockMvc.perform(put("/api/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "Sarah J. Jenkins",
                                  "phone": "555-9999",
                                  "avatarUrl": "https://example.com/sarah.png",
                                  "currentPassword": "Password123!",
                                  "newPassword": "NewPassword456!"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Sarah J. Jenkins"))
                .andExpect(jsonPath("$.phone").value("555-9999"))
                .andExpect(jsonPath("$.avatarUrl").value("https://example.com/sarah.png"));

        // Verify new password works on login
        String newToken = loginAndGetToken(email, "NewPassword456!");
        assertThat(newToken).isNotBlank();

        // 5. CUSTOMER cannot access admin-only endpoints (403)
        mockMvc.perform(get("/api/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + newToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));

        mockMvc.perform(post("/api/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + newToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "Unauthorized Vet",
                                  "email": "unauth.vet@meocare.clinic",
                                  "password": "Password123!",
                                  "role": "VETERINARIAN"
                                }
                                """))
                .andExpect(status().isForbidden());

        // 6. CUSTOMER can list active veterinarians
        mockMvc.perform(get("/api/users/veterinarians")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + newToken))
                .andExpect(status().isOk());
    }

    @Test
    void adminCanProvisionVeterinarianFilterUsersAndLockAccount() throws Exception {
        String adminToken = loginAndGetToken("admin@meocare.clinic", "Admin@123456");
        String vetEmail = "dr.vance." + System.nanoTime() + "@meocare.clinic";

        // Admin creates VETERINARIAN
        MvcResult createResult = mockMvc.perform(post("/api/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "Dr. Emily Vance",
                                  "email": "%s",
                                  "password": "VetPassword123!",
                                  "phone": "555-0200",
                                  "role": "VETERINARIAN"
                                }
                                """.formatted(vetEmail)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("VETERINARIAN"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andReturn();

        JsonNode createdVet = objectMapper.readTree(createResult.getResponse().getContentAsString());
        long vetId = createdVet.get("id").asLong();

        // Admin cannot provision CUSTOMER via POST /api/users
        mockMvc.perform(post("/api/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "Wrong Role",
                                  "email": "wrong.role@meocare.clinic",
                                  "password": "Password123!",
                                  "role": "CUSTOMER"
                                }
                                """))
                .andExpect(status().isBadRequest());

        // Admin filters users by role
        mockMvc.perform(get("/api/users")
                        .param("role", "VETERINARIAN")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].role").value("VETERINARIAN"));

        // Vet logs in and gets token
        String vetToken = loginAndGetToken(vetEmail, "VetPassword123!");
        mockMvc.perform(get("/api/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + vetToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("VETERINARIAN"));

        // Admin locks vet account
        mockMvc.perform(patch("/api/users/{id}/status", vetId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "status": "LOCKED" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("LOCKED"));

        // Locked vet's existing JWT is rejected with 403
        mockMvc.perform(get("/api/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + vetToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));

        // Locked vet cannot log in again (403)
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "VetPassword123!"
                                }
                                """.formatted(vetEmail)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    private String loginAndGetToken(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "%s"
                                }
                                """.formatted(email, password)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.get("token").asString();
    }
}
