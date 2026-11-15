package com.foodbridge.request.dto;

import com.foodbridge.request.NgoRequest;
import com.foodbridge.request.NgoRequestStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record NgoRequestResponse(
        Long id,
        String title,
        String description,
        String categoryName,
        BigDecimal quantityNeeded,
        String quantityUnit,
        String city,
        Instant neededBy,
        int beneficiaries,
        NgoRequestStatus status,
        Long ngoId,
        String ngoName,
        String ngoEmail,
        Instant createdAt
) {
    public static NgoRequestResponse from(NgoRequest r) {
        return new NgoRequestResponse(
                r.getId(), r.getTitle(), r.getDescription(),
                r.getCategory() == null ? null : r.getCategory().getName(),
                r.getQuantityNeeded(), r.getQuantityUnit(), r.getCity(), r.getNeededBy(),
                r.getBeneficiaries(), r.getStatus(), r.getNgo().getId(), r.getNgo().getFullName(),
                r.getNgo().getEmail(), r.getCreatedAt());
    }
}
