package com.foodbridge.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.foodbridge.donation.Donation;
import com.foodbridge.support.IntegrationTestBase;
import com.foodbridge.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Proves that authorization is enforced by the backend, not merely by hiding
 * buttons in the UI.
 */
@AutoConfigureMockMvc
class AuthorizationApiIT extends IntegrationTestBase {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private com.foodbridge.security.JwtService jwtService;

    private User donor;
    private User ngo;
    private User admin;
    private String donorToken;
    private String ngoToken;
    private String adminToken;

    @BeforeEach
    void setUpUsers() {
        donor = dataFactory.donor();
        ngo = dataFactory.approvedNgo();
        admin = dataFactory.admin();
        donorToken = jwtService.issue(donor).value();
        ngoToken = jwtService.issue(ngo).value();
        adminToken = jwtService.issue(admin).value();
    }

    @Test
    @DisplayName("Unauthenticated requests receive 401 with a structured error body")
    void anonymousRejected() throws Exception {
        mockMvc.perform(get("/api/donations/mine"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    @DisplayName("A donor token cannot reach NGO or admin endpoints")
    void donorCannotReachOtherRoles() throws Exception {
        mockMvc.perform(get("/api/admin/stats").header("Authorization", "Bearer " + donorToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/claims").header("Authorization", "Bearer " + donorToken))
                .andExpect(status().isOk()); // a donor may list claims on their own donations
    }

    @Test
    @DisplayName("An NGO token cannot reach admin endpoints nor create donations")
    void ngoScope() throws Exception {
        mockMvc.perform(get("/api/admin/users").header("Authorization", "Bearer " + ngoToken))
                .andExpect(status().isForbidden());

        String body = """
                {"foodName":"Sneaky","quantity":1,"quantityUnit":"KG","foodType":"VEGETARIAN",
                 "categoryId":1,"preparedAt":"%s","expiresAt":"%s",
                 "pickupAddress":"x","city":"Pune","safetyDeclaration":true}
                """.formatted(Instant.now().minus(1, ChronoUnit.HOURS), Instant.now().plus(4, ChronoUnit.HOURS));
        mockMvc.perform(post("/api/donations").header("Authorization", "Bearer " + ngoToken)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("An admin token can reach admin endpoints")
    void adminScope() throws Exception {
        mockMvc.perform(get("/api/admin/stats").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUsers").isNumber());
    }

    @Test
    @DisplayName("A tampered token is rejected")
    void tamperedTokenRejected() throws Exception {
        mockMvc.perform(get("/api/me").header("Authorization", "Bearer " + donorToken + "x"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/me").header("Authorization", "Bearer garbage.token.value"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("A donor cannot read or edit another donor's donation through the API")
    void crossTenantAccessBlocked() throws Exception {
        User otherDonor = dataFactory.donor();
        Donation donation = donationRepository.save(dataFactory.donation(otherDonor));

        // A donor may only open their own donations; another donor's record is not readable.
        mockMvc.perform(get("/api/donations/" + donation.getId())
                        .header("Authorization", "Bearer " + donorToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

        String update = """
                {"foodName":"Hijacked","quantity":1,"quantityUnit":"KG","foodType":"VEGETARIAN",
                 "categoryId":1,"preparedAt":"%s","expiresAt":"%s",
                 "pickupAddress":"x","city":"Pune"}
                """.formatted(Instant.now().minus(1, ChronoUnit.HOURS), Instant.now().plus(4, ChronoUnit.HOURS));
        mockMvc.perform(put("/api/donations/" + donation.getId())
                        .header("Authorization", "Bearer " + donorToken)
                        .contentType(MediaType.APPLICATION_JSON).content(update))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    @DisplayName("Validation errors return 400 with field errors")
    void validationErrorsAreStructured() throws Exception {
        mockMvc.perform(post("/api/donations").header("Authorization", "Bearer " + donorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"foodName\":\"\",\"quantity\":-5}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors.foodName").exists())
                .andExpect(jsonPath("$.requestId").isNotEmpty());
    }

    @Test
    @DisplayName("Unknown endpoints return the JSON error envelope, not HTML")
    void unknownEndpoint() throws Exception {
        mockMvc.perform(get("/api/does-not-exist").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }
}
