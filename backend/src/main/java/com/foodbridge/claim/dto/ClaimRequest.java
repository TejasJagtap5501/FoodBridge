package com.foodbridge.claim.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Size;

/**
 * NGO claim payload.
 *
 * @param message        optional note to the donor
 * @param suitabilityCheck NGO's declaration that it has assessed the food's
 *                         suitability for its beneficiaries (required by policy)
 */
public record ClaimRequest(
        @Size(max = 500) String message,
        @AssertTrue(message = "you must confirm that your organisation has assessed the food's suitability")
        boolean suitabilityCheck
) {
}
