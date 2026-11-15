package com.foodbridge.donation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the impact calculation used by dashboards and reports.
 */
class MealEstimationTest {

    @Test
    @DisplayName("Kilograms convert at 0.4 kg per meal")
    void kilograms() {
        assertThat(DonationService.estimateMeals(new BigDecimal("4"), "KG")).isEqualTo(10);
        assertThat(DonationService.estimateMeals(new BigDecimal("12"), "kg")).isEqualTo(30);
        assertThat(DonationService.estimateMeals(new BigDecimal("1"), "KG")).isEqualTo(3);
    }

    @Test
    @DisplayName("Grams are normalised to kilograms")
    void grams() {
        assertThat(DonationService.estimateMeals(new BigDecimal("2000"), "G")).isEqualTo(5);
    }

    @Test
    @DisplayName("Countable units already represent portions")
    void packets() {
        assertThat(DonationService.estimateMeals(new BigDecimal("10"), "PACKETS")).isEqualTo(10);
        assertThat(DonationService.estimateMeals(new BigDecimal("10"), "SERVINGS")).isEqualTo(10);
        assertThat(DonationService.estimateMeals(new BigDecimal("25"), "MEALS")).isEqualTo(25);
        assertThat(DonationService.estimateMeals(new BigDecimal("4"), "BOXES")).isEqualTo(4);
    }

    @Test
    @DisplayName("An unknown unit is treated as kilograms")
    void unknownUnit() {
        assertThat(DonationService.estimateMeals(new BigDecimal("2"), "TRAYS")).isEqualTo(5);
    }

    @Test
    @DisplayName("Tiny quantities still count as at least one meal and null is safe")
    void edgeCases() {
        assertThat(DonationService.estimateMeals(new BigDecimal("0.05"), "KG")).isEqualTo(1);
        assertThat(DonationService.estimateMeals(null, "KG")).isZero();
    }
}
