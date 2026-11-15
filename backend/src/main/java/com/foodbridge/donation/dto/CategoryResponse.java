package com.foodbridge.donation.dto;

import com.foodbridge.donation.FoodCategory;

public record CategoryResponse(
        Long id,
        String name,
        String slug,
        String description,
        boolean active,
        int displayOrder
) {
    public static CategoryResponse from(FoodCategory c) {
        return new CategoryResponse(c.getId(), c.getName(), c.getSlug(), c.getDescription(),
                c.isActive(), c.getDisplayOrder());
    }
}
