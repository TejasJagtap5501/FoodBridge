package com.foodbridge.audit;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Canonical audit action names. Using constants avoids typos and makes the
 * audit log queryable.
 */
@Getter
@RequiredArgsConstructor
public enum AuditAction {
    NGO_APPROVED("NGO_APPROVED"),
    NGO_REJECTED("NGO_REJECTED"),
    USER_ACTIVATED("USER_ACTIVATED"),
    USER_DEACTIVATED("USER_DEACTIVATED"),
    DONATION_MODERATED("DONATION_MODERATED"),
    CATEGORY_CREATED("CATEGORY_CREATED"),
    CATEGORY_UPDATED("CATEGORY_UPDATED"),
    CATEGORY_DELETED("CATEGORY_DELETED"),
    CLAIM_FORCE_CANCELLED("CLAIM_FORCE_CANCELLED"),
    PASSWORD_RESET_COMPLETED("PASSWORD_RESET_COMPLETED");

    private final String code;
}
