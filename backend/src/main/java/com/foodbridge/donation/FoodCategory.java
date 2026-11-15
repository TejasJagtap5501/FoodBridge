package com.foodbridge.donation;

import com.foodbridge.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Admin managed food category (Cooked Meals, Bakery, ...).
 */
@Entity
@Table(name = "food_categories")
@Getter
@Setter
@NoArgsConstructor
public class FoodCategory extends BaseEntity {

    @Column(nullable = false, length = 80, unique = true)
    private String name;

    @Column(nullable = false, length = 80, unique = true)
    private String slug;

    @Column(length = 255)
    private String description;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "display_order", nullable = false)
    private int displayOrder = 0;
}
