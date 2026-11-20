package com.meocare.meocare.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ClinicalWorkflowIntegrationTest {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void petOwnershipEnforcementAndEmbeddedImagesLifecycle() throws Exception {
        String adminToken = loginAndGetToken("admin@meocare.clinic", "Admin@123456");
        TestAccount customerA = registerAndLoginCustomer("owner.a");
        TestAccount customerB = registerAndLoginCustomer("owner.b");
        TestAccount vet = createAndLoginVet(adminToken, "vet.pet");

        // Customer A creates pet without ownerId -> automatically derived from JWT
        MvcResult petRes = mockMvc.perform(post("/api/pets")
                        .header(HttpHeaders.AUTHORIZATION, bearer(customerA.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Luna",
                                  "species": "Cat",
                                  "breed": "Ragdoll",
                                  "gender": "FEMALE",
                                  "dateOfBirth": "2023-06-14",
                                  "weight": 4.20,
                                  "description": "Mild dermatitis history"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ownerId").value(customerA.id()))
                .andExpect(jsonPath("$.images").isArray())
                .andReturn();
        long petId = objectMapper.readTree(petRes.getResponse().getContentAsString()).get("id").asLong();

        // Customer A adds image
        MvcResult imgRes = mockMvc.perform(post("/api/pets/{petId}/images", petId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(customerA.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "imageUrl": "https://example.com/luna-1.jpg" }
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        long imageId = objectMapper.readTree(imgRes.getResponse().getContentAsString()).get("id").asLong();

        // Embedded image returned in GET /api/pets/{id}
        mockMvc.perform(get("/api/pets/{id}", petId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(customerA.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.images.length()").value(1))
                .andExpect(jsonPath("$.images[0].imageUrl").value("https://example.com/luna-1.jpg"));

        // Customer B gets 403 on read/update/delete/image
        mockMvc.perform(get("/api/pets/{id}", petId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(customerB.token())))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/pets/{id}", petId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(customerB.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "name": "Hacked", "species": "Cat" }
                                """))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/pets/{id}", petId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(customerB.token())))
                .andExpect(status().isForbidden());

        // Vet & Admin can read Customer A's pet
        mockMvc.perform(get("/api/pets/{id}", petId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(vet.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Luna"));

        // Customer A deletes image
        mockMvc.perform(delete("/api/pets/{petId}/images/{imageId}", petId, imageId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(customerA.token())))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/pets/{id}", petId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(customerA.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.images.length()").value(0));
    }

    @Test
    void fullAppointmentNegotiationConflictAndMedicalTimelineWorkflow() throws Exception {
        String adminToken = loginAndGetToken("admin@meocare.clinic", "Admin@123456");
        TestAccount customerA = registerAndLoginCustomer("workflow.a");
        TestAccount customerB = registerAndLoginCustomer("workflow.b");
        TestAccount vet = createAndLoginVet(adminToken, "vet.workflow");

        long petId = createPet(customerA.token(), "Milo", "Dog");

        LocalDateTime reqStart = LocalDateTime.now().plusDays(3).withHour(9).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime reqEnd = reqStart.plusMinutes(45);

        // 1. Customer creates appointment with imageUrls -> persisted & returned
        MvcResult apptRes = mockMvc.perform(post("/api/appointments")
                        .header(HttpHeaders.AUTHORIZATION, bearer(customerA.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "petId": %d,
                                  "requestedStart": "%s",
                                  "requestedEnd": "%s",
                                  "symptoms": "Scratching left ear",
                                  "imageUrls": ["https://example.com/symptom-ear.jpg"]
                                }
                                """.formatted(petId, ISO.format(reqStart), ISO.format(reqEnd))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.images.length()").value(1))
                .andExpect(jsonPath("$.images[0].imageUrl").value("https://example.com/symptom-ear.jpg"))
                .andReturn();
        long apptId = objectMapper.readTree(apptRes.getResponse().getContentAsString()).get("id").asLong();

        // Admin cannot assign a CUSTOMER as veterinarian (400)
        mockMvc.perform(patch("/api/appointments/{id}/assign", apptId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "veterinarianId": %d }
                                """.formatted(customerB.id())))
                .andExpect(status().isBadRequest());

        // Admin assigns active VETERINARIAN
        mockMvc.perform(patch("/api/appointments/{id}/assign", apptId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "veterinarianId": %d }
                                """.formatted(vet.id())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.veterinarianId").value(vet.id()));

        // 2. Vet proposes alternative time via /api/appointments/{id}/proposals -> RESCHEDULE_PROPOSED
        LocalDateTime prop1Start = reqStart.plusDays(1);
        LocalDateTime prop1End = prop1Start.plusMinutes(45);
        MvcResult prop1Res = mockMvc.perform(post("/api/appointments/{id}/proposals", apptId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(vet.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "proposedStart": "%s",
                                  "proposedEnd": "%s",
                                  "note": "In surgery on requested day"
                                }
                                """.formatted(ISO.format(prop1Start), ISO.format(prop1End))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.proposedById").value(vet.id()))
                .andExpect(jsonPath("$.proposedByRole").value("VETERINARIAN"))
                .andReturn();
        long prop1Id = objectMapper.readTree(prop1Res.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(get("/api/appointments/{id}", apptId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(customerA.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESCHEDULE_PROPOSED"));

        // Vet cannot accept their own proposal -> 403
        mockMvc.perform(patch("/api/appointments/{apptId}/proposals/{propId}/accept", apptId, prop1Id)
                        .header(HttpHeaders.AUTHORIZATION, bearer(vet.token())))
                .andExpect(status().isForbidden());

        // 3. Customer counter-proposes via /api/appointment-proposals
        LocalDateTime prop2Start = reqStart.plusDays(2).withHour(10).withMinute(15);
        LocalDateTime prop2End = prop2Start.plusMinutes(45);
        MvcResult prop2Res = mockMvc.perform(post("/api/appointment-proposals")
                        .header(HttpHeaders.AUTHORIZATION, bearer(customerA.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "appointmentId": %d,
                                  "proposedStart": "%s",
                                  "proposedEnd": "%s",
                                  "note": "Friday morning works best"
                                }
                                """.formatted(apptId, ISO.format(prop2Start), ISO.format(prop2End))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.proposedById").value(customerA.id()))
                .andExpect(jsonPath("$.proposedByRole").value("CUSTOMER"))
                .andReturn();
        long prop2Id = objectMapper.readTree(prop2Res.getResponse().getContentAsString()).get("id").asLong();

        // 4. Vet accepts Customer's counter-proposal -> CONFIRMED & prop1 auto-DECLINED
        mockMvc.perform(patch("/api/appointments/{apptId}/proposals/{propId}/accept", apptId, prop2Id)
                        .header(HttpHeaders.AUTHORIZATION, bearer(vet.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));

        mockMvc.perform(get("/api/appointments/{id}", apptId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(customerA.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.confirmedStart").value(ISO.format(prop2Start)));

        // Verify competing proposal #1 was auto-declined
        MvcResult proposalsRes = mockMvc.perform(get("/api/appointments/{id}/proposals", apptId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(customerA.token())))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode proposals = objectMapper.readTree(proposalsRes.getResponse().getContentAsString());
        assertThat(proposals.size()).isEqualTo(2);
        for (JsonNode p : proposals) {
            if (p.get("id").asLong() == prop1Id) {
                assertThat(p.get("status").asString()).isEqualTo("DECLINED");
            } else if (p.get("id").asLong() == prop2Id) {
                assertThat(p.get("status").asString()).isEqualTo("ACCEPTED");
            }
        }

        // 5. Schedule conflict test: overlapping appointment for same vet -> 409 Conflict
        long petBId = createPet(customerB.token(), "Rocky", "Dog");
        LocalDateTime overlapStart = prop2Start.plusMinutes(15);
        LocalDateTime overlapEnd = overlapStart.plusMinutes(30);
        MvcResult conflictApptRes = mockMvc.perform(post("/api/appointments")
                        .header(HttpHeaders.AUTHORIZATION, bearer(customerB.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "petId": %d,
                                  "veterinarianId": %d,
                                  "requestedStart": "%s",
                                  "requestedEnd": "%s",
                                  "symptoms": "Overlapping checkup"
                                }
                                """.formatted(petBId, vet.id(), ISO.format(overlapStart), ISO.format(overlapEnd))))
                .andExpect(status().isCreated())
                .andReturn();
        long conflictApptId = objectMapper.readTree(conflictApptRes.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(patch("/api/appointments/{id}/confirm", conflictApptId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(vet.token())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));

        // Creating medical record on PENDING appointment -> 409 Conflict
        mockMvc.perform(post("/api/medical-records")
                        .header(HttpHeaders.AUTHORIZATION, bearer(vet.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "appointmentId": %d,
                                  "weight": 20.0,
                                  "temperature": 38.5,
                                  "diagnosis": "Premature record"
                                }
                                """.formatted(conflictApptId)))
                .andExpect(status().isConflict());

        // Rejecting the conflicting appointment succeeds -> REJECTED
        mockMvc.perform(patch("/api/appointments/{id}/reject", conflictApptId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(vet.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));

        // 6. Vet completes appointment #1 -> terminal state blocks further transitions (409)
        mockMvc.perform(patch("/api/appointments/{id}/status", apptId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(vet.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "status": "COMPLETED" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        mockMvc.perform(patch("/api/appointments/{id}/cancel", apptId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(customerA.token())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));

        // 7. Medical record RBAC & creation with embedded images
        mockMvc.perform(post("/api/medical-records")
                        .header(HttpHeaders.AUTHORIZATION, bearer(customerA.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "appointmentId": %d, "diagnosis": "Customer cannot write" }
                                """.formatted(apptId)))
                .andExpect(status().isForbidden());

        MvcResult recordRes = mockMvc.perform(post("/api/medical-records")
                        .header(HttpHeaders.AUTHORIZATION, bearer(vet.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "appointmentId": %d,
                                  "weight": 31.40,
                                  "temperature": 38.6,
                                  "examination": "Mild pinna erythema",
                                  "diagnosis": "Allergic otitis externa",
                                  "treatment": "Ear flush and topical drops",
                                  "advice": "Keep ear dry for 5 days",
                                  "notes": "Follow up in 1 week",
                                  "imageUrls": ["https://example.com/exam-ear.jpg"]
                                }
                                """.formatted(apptId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.images.length()").value(1))
                .andExpect(jsonPath("$.veterinarianId").value(vet.id()))
                .andReturn();
        long recordId = objectMapper.readTree(recordRes.getResponse().getContentAsString()).get("id").asLong();

        // 8. Daily Health Update RBAC, attribution & timeline endpoint
        mockMvc.perform(post("/api/medical-records/{id}/health-updates", recordId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(customerA.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "healthStatus": "STABLE", "description": "Unauthorized update" }
                                """))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/medical-records/{id}/health-updates", recordId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(vet.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "healthStatus": "IMPROVING",
                                  "description": "Redness noticeably reduced; appetite normal.",
                                  "imageUrls": ["https://example.com/recovery-day1.jpg"]
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.veterinarianId").value(vet.id()))
                .andExpect(jsonPath("$.petId").value(petId))
                .andExpect(jsonPath("$.images.length()").value(1));

        // Customer A views chronological health timeline
        mockMvc.perform(get("/api/health-updates")
                        .param("petId", String.valueOf(petId))
                        .header(HttpHeaders.AUTHORIZATION, bearer(customerA.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].healthStatus").value("IMPROVING"))
                .andExpect(jsonPath("$[0].images[0].imageUrl").value("https://example.com/recovery-day1.jpg"));

        // Customer B gets 403 on Customer A's medical record and health timeline
        mockMvc.perform(get("/api/medical-records/{id}", recordId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(customerB.token())))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/health-updates")
                        .param("petId", String.valueOf(petId))
                        .header(HttpHeaders.AUTHORIZATION, bearer(customerB.token())))
                .andExpect(status().isForbidden());

        // Vet/Admin can list all clinic medical records without petId
        mockMvc.perform(get("/api/medical-records")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(recordId));
    }

    @Test
    void localImageUploadValidatesMagicBytesAndServesPublicly() throws Exception {
        TestAccount customer = registerAndLoginCustomer("uploader");

        // Minimal valid 1x1 PNG header + bytes
        byte[] validPng = new byte[]{
                (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
                0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52
        };
        MockMultipartFile pngFile = new MockMultipartFile(
                "file", "luna.png", "image/png", validPng);

        MvcResult uploadRes = mockMvc.perform(multipart("/api/uploads/images")
                        .file(pngFile)
                        .header(HttpHeaders.AUTHORIZATION, bearer(customer.token())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.contentType").value("image/png"))
                .andReturn();

        String uploadedUrl = objectMapper.readTree(uploadRes.getResponse().getContentAsString()).get("url").asString();
        assertThat(uploadedUrl).startsWith("/uploads/").endsWith(".png");

        // Uploaded file is publicly served under /uploads/**
        mockMvc.perform(get(uploadedUrl))
                .andExpect(status().isOk());

        // Spoofed PNG (plain text bytes with image/png header) -> 400 Bad Request
        MockMultipartFile spoofedPng = new MockMultipartFile(
                "file", "fake.png", "image/png", "not-actually-a-png".getBytes());
        mockMvc.perform(multipart("/api/uploads/images")
                        .file(spoofedPng)
                        .header(HttpHeaders.AUTHORIZATION, bearer(customer.token())))
                .andExpect(status().isBadRequest());

        // Unsupported MIME type -> 400 Bad Request
        MockMultipartFile textFile = new MockMultipartFile(
                "file", "script.txt", "text/plain", "hello".getBytes());
        mockMvc.perform(multipart("/api/uploads/images")
                        .file(textFile)
                        .header(HttpHeaders.AUTHORIZATION, bearer(customer.token())))
                .andExpect(status().isBadRequest());
    }

    // ----------------------------------------------------------------- helpers

    private record TestAccount(long id, String email, String token) {
    }

    private TestAccount registerAndLoginCustomer(String prefix) throws Exception {
        String email = prefix + "." + System.nanoTime() + "@oakmail.com";
        MvcResult reg = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "Customer %s",
                                  "email": "%s",
                                  "password": "Password123!",
                                  "phone": "555-0101"
                                }
                                """.formatted(prefix, email)))
                .andExpect(status().isCreated())
                .andReturn();
        long id = objectMapper.readTree(reg.getResponse().getContentAsString()).get("id").asLong();
        String token = loginAndGetToken(email, "Password123!");
        return new TestAccount(id, email, token);
    }

    private TestAccount createAndLoginVet(String adminToken, String prefix) throws Exception {
        String email = prefix + "." + System.nanoTime() + "@meocare.clinic";
        MvcResult res = mockMvc.perform(post("/api/users")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "Dr. %s",
                                  "email": "%s",
                                  "password": "VetPassword123!",
                                  "role": "VETERINARIAN"
                                }
                                """.formatted(prefix, email)))
                .andExpect(status().isCreated())
                .andReturn();
        long id = objectMapper.readTree(res.getResponse().getContentAsString()).get("id").asLong();
        String token = loginAndGetToken(email, "VetPassword123!");
        return new TestAccount(id, email, token);
    }

    private long createPet(String token, String name, String species) throws Exception {
        MvcResult res = mockMvc.perform(post("/api/pets")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "name": "%s", "species": "%s", "gender": "MALE" }
                                """.formatted(name, species)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(res.getResponse().getContentAsString()).get("id").asLong();
    }

    private String loginAndGetToken(String email, String password) throws Exception {
        MvcResult res = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "email": "%s", "password": "%s" }
                                """.formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(res.getResponse().getContentAsString()).get("token").asString();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
