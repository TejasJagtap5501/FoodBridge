package com.foodbridge.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.foodbridge.auth.OneTimeTokenRepository;
import com.foodbridge.auth.RefreshTokenRepository;
import com.foodbridge.auth.TokenPurpose;
import com.foodbridge.support.IntegrationTestBase;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * HTTP-level tests for registration, login, refresh and password reset.
 * These exercise the real filter chain, so content negotiation, status codes and
 * cookie handling are all verified end to end.
 */
@AutoConfigureMockMvc
class AuthApiIT extends IntegrationTestBase {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private RefreshTokenRepository refreshTokenRepository;
    @Autowired private OneTimeTokenRepository oneTimeTokenRepository;

    private static final String DONOR_JSON = """
            {
              "email": "newdonor@example.org",
              "password": "StrongPass123",
              "fullName": "New Donor",
              "phone": "+91 98765 43210",
              "businessName": "Test Bistro",
              "businessType": "CAFE",
              "contactPerson": "New Donor",
              "addressLine1": "5 Test Lane",
              "city": "Pune",
              "state": "Maharashtra",
              "postalCode": "411001"
            }
            """;

    private static final String NGO_JSON = """
            {
              "email": "newngo@example.org",
              "password": "StrongPass123",
              "fullName": "NGO Contact",
              "organizationName": "Helping Hands Trust",
              "registrationNumber": "MH/2025/000111",
              "contactPerson": "NGO Contact",
              "addressLine1": "9 Charity Road",
              "city": "Pune",
              "state": "Maharashtra",
              "postalCode": "411002"
            }
            """;

    @Test
    @DisplayName("Donor registration returns 201, an access token and an HttpOnly refresh cookie")
    void donorRegistration() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/register/donor")
                        .contentType(MediaType.APPLICATION_JSON).content(DONOR_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.user.email").value("newdonor@example.org"))
                .andExpect(jsonPath("$.user.role").value("DONOR"))
                .andExpect(jsonPath("$.user.passwordHash").doesNotExist())
                .andReturn();

        Cookie refresh = result.getResponse().getCookie("fb_refresh");
        assertThat(refresh).isNotNull();
        assertThat(refresh.isHttpOnly()).isTrue();
        assertThat(refresh.getPath()).isEqualTo("/api/auth");

        assertThat(userRepository.findByEmailIgnoreCase("newdonor@example.org")).isPresent();
        assertThat(donorProfileRepository.findAll()).hasSize(1);
    }

    @Test
    @DisplayName("Duplicate email registration is rejected with 409")
    void duplicateEmail() throws Exception {
        mockMvc.perform(post("/api/auth/register/donor")
                        .contentType(MediaType.APPLICATION_JSON).content(DONOR_JSON))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/register/donor")
                        .contentType(MediaType.APPLICATION_JSON).content(DONOR_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_TAKEN"));
    }

    @Test
    @DisplayName("Weak passwords are rejected with a field-level explanation")
    void weakPasswordRejected() throws Exception {
        String weak = DONOR_JSON.replace("StrongPass123", "weak");
        mockMvc.perform(post("/api/auth/register/donor")
                        .contentType(MediaType.APPLICATION_JSON).content(weak))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors.password").exists());
    }

    @Test
    @DisplayName("Invalid email format is rejected by bean validation")
    void invalidEmailRejected() throws Exception {
        String bad = DONOR_JSON.replace("newdonor@example.org", "not-an-email");
        mockMvc.perform(post("/api/auth/register/donor")
                        .contentType(MediaType.APPLICATION_JSON).content(bad))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.email").exists());
    }

    @Test
    @DisplayName("NGO registration creates a PENDING account that cannot claim yet")
    void ngoRegistrationIsPending() throws Exception {
        mockMvc.perform(post("/api/auth/register/ngo")
                        .contentType(MediaType.APPLICATION_JSON).content(NGO_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.status").value("PENDING"));

        var user = userRepository.findByEmailIgnoreCase("newngo@example.org").orElseThrow();
        assertThat(ngoProfileRepository.findByUserId(user.getId()).orElseThrow().getVerificationStatus().name())
                .isEqualTo("PENDING");
    }

    @Test
    @DisplayName("Login succeeds with correct credentials and fails with wrong ones")
    void login() throws Exception {
        mockMvc.perform(post("/api/auth/register/donor")
                .contentType(MediaType.APPLICATION_JSON).content(DONOR_JSON)).andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"newdonor@example.org\",\"password\":\"StrongPass123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"newdonor@example.org\",\"password\":\"WrongPass123\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"ghost@example.org\",\"password\":\"WrongPass123\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    @DisplayName("Refresh rotates the cookie and invalidates the previous token")
    void refreshRotates() throws Exception {
        MvcResult registration = mockMvc.perform(post("/api/auth/register/donor")
                        .contentType(MediaType.APPLICATION_JSON).content(DONOR_JSON))
                .andExpect(status().isCreated()).andReturn();
        Cookie first = registration.getResponse().getCookie("fb_refresh");
        assertThat(first).isNotNull();

        MvcResult refreshed = mockMvc.perform(post("/api/auth/refresh").cookie(first))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andReturn();
        Cookie second = refreshed.getResponse().getCookie("fb_refresh");
        assertThat(second.getValue()).isNotEqualTo(first.getValue());

        // Replaying the rotated token must fail.
        mockMvc.perform(post("/api/auth/refresh").cookie(first))
                .andExpect(status().isUnauthorized());

        assertThat(refreshTokenRepository.findAll())
                .as("registration, refresh and login each issue a refresh token")
                .hasSizeGreaterThanOrEqualTo(2);
        assertThat(refreshTokenRepository.findAll().stream().filter(t -> t.isRevoked()).count())
                .as("the rotated token is revoked")
                .isGreaterThanOrEqualTo(1);
    }

    @Test
    @DisplayName("Logout revokes the refresh token and clears the cookie")
    void logout() throws Exception {
        MvcResult registration = mockMvc.perform(post("/api/auth/register/donor")
                        .contentType(MediaType.APPLICATION_JSON).content(DONOR_JSON))
                .andExpect(status().isCreated()).andReturn();
        Cookie cookie = registration.getResponse().getCookie("fb_refresh");

        mockMvc.perform(post("/api/auth/logout").cookie(cookie))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/refresh").cookie(cookie))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Password reset flow: request → token → reset → old password no longer works")
    void passwordReset() throws Exception {
        mockMvc.perform(post("/api/auth/register/donor")
                .contentType(MediaType.APPLICATION_JSON).content(DONOR_JSON)).andExpect(status().isCreated());

        MvcResult forgot = mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"newdonor@example.org\"}"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode body = objectMapper.readTree(forgot.getResponse().getContentAsString());
        String resetToken = body.get("resetToken").asText();
        assertThat(resetToken).isNotBlank();
        assertThat(oneTimeTokenRepository.findAll())
                .anyMatch(t -> t.getPurpose() == TokenPurpose.PASSWORD_RESET);

        mockMvc.perform(post("/api/auth/reset-password").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + resetToken + "\",\"newPassword\":\"BrandNewPass99\"}"))
                .andExpect(status().isOk());

        // Completing a reset revokes every refresh token issued before it.
        assertThat(refreshTokenRepository.findAll())
                .as("all pre-reset refresh tokens are revoked")
                .noneMatch(t -> !t.isRevoked());

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"newdonor@example.org\",\"password\":\"StrongPass123\"}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"newdonor@example.org\",\"password\":\"BrandNewPass99\"}"))
                .andExpect(status().isOk());

        // A used reset token cannot be replayed.
        mockMvc.perform(post("/api/auth/reset-password").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + resetToken + "\",\"newPassword\":\"AnotherPass77\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Forgot-password does not reveal whether an account exists")
    void forgotPasswordDoesNotEnumerate() throws Exception {
        mockMvc.perform(post("/api/auth/forgot-password").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"nobody@example.org\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("If an account exists")));
    }
}
