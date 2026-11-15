package com.foodbridge.admin;

import com.foodbridge.admin.dto.AccountStatusRequest;
import com.foodbridge.admin.dto.NgoVerificationResponse;
import com.foodbridge.admin.dto.VerificationDecisionRequest;
import com.foodbridge.exception.ApiException;
import com.foodbridge.support.IntegrationTestBase;
import com.foodbridge.support.TestPrincipals;
import com.foodbridge.user.User;
import com.foodbridge.user.UserStatus;
import com.foodbridge.user.VerificationStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.*;

/**
 * NGO verification workflow and account administration, including the audit trail.
 */
class AdminVerificationIT extends IntegrationTestBase {

    @Autowired private AdminService adminService;
    @Autowired private com.foodbridge.audit.AuditLogRepository auditLogRepository;

    @Test
    @DisplayName("Approving an NGO activates the account and records an audit entry")
    void approveNgo() {
        User admin = dataFactory.admin();
        User pending = dataFactory.ngo(VerificationStatus.PENDING);
        Long profileId = ngoProfileRepository.findByUserId(pending.getId()).orElseThrow().getId();

        NgoVerificationResponse response = adminService.decideVerification(
                TestPrincipals.of(admin), profileId, new VerificationDecisionRequest(true, "Papers verified"), "127.0.0.1");

        assertThat(response.verificationStatus()).isEqualTo(VerificationStatus.APPROVED);
        assertThat(userRepository.findById(pending.getId()).orElseThrow().getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(auditLogRepository.findAll())
                .anyMatch(a -> a.getAction().equals("NGO_APPROVED") && a.getEntityId().equals(profileId));
    }

    @Test
    @DisplayName("Rejecting an NGO deactivates the account and stores the reason")
    void rejectNgo() {
        User admin = dataFactory.admin();
        User pending = dataFactory.ngo(VerificationStatus.PENDING);
        Long profileId = ngoProfileRepository.findByUserId(pending.getId()).orElseThrow().getId();

        NgoVerificationResponse response = adminService.decideVerification(
                TestPrincipals.of(admin), profileId,
                new VerificationDecisionRequest(false, "Registration number could not be verified"), "127.0.0.1");

        assertThat(response.verificationStatus()).isEqualTo(VerificationStatus.REJECTED);
        assertThat(response.verificationNote()).contains("Registration number");
        assertThat(userRepository.findById(pending.getId()).orElseThrow().getStatus()).isEqualTo(UserStatus.INACTIVE);
    }

    @Test
    @DisplayName("A decided application cannot be decided a second time")
    void doubleDecisionRejected() {
        User admin = dataFactory.admin();
        User pending = dataFactory.ngo(VerificationStatus.PENDING);
        Long profileId = ngoProfileRepository.findByUserId(pending.getId()).orElseThrow().getId();

        adminService.decideVerification(TestPrincipals.of(admin), profileId,
                new VerificationDecisionRequest(true, null), "127.0.0.1");

        assertThatThrownBy(() -> adminService.decideVerification(TestPrincipals.of(admin), profileId,
                new VerificationDecisionRequest(false, null), "127.0.0.1"))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getCode()).isEqualTo("ALREADY_DECIDED"));
    }

    @Test
    @DisplayName("Administrators cannot deactivate their own account")
    void adminCannotModifySelf() {
        User admin = dataFactory.admin();
        assertThatThrownBy(() -> adminService.updateAccountStatus(TestPrincipals.of(admin), admin.getId(),
                new AccountStatusRequest(UserStatus.INACTIVE, "test"), "127.0.0.1"))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getCode()).isEqualTo("SELF_MODIFICATION"));
    }

    @Test
    @DisplayName("Platform statistics reflect real database rows")
    void platformStats() {
        dataFactory.donor();
        dataFactory.donor();
        dataFactory.approvedNgo();
        dataFactory.ngo(VerificationStatus.PENDING);
        dataFactory.admin();

        var stats = adminService.platformStats();
        assertThat(stats.totalUsers()).isEqualTo(5);
        assertThat(stats.totalDonors()).isEqualTo(2);
        assertThat(stats.totalNgos()).isEqualTo(2);
        assertThat(stats.pendingNgoVerifications()).isEqualTo(1);
        assertThat(stats.approvedNgos()).isEqualTo(1);
        assertThat(stats.totalDonations()).isZero();
    }
}
