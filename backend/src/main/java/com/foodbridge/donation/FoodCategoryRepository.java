package com.foodbridge.donation;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FoodCategoryRepository extends JpaRepository<FoodCategory, Long> {

    List<FoodCategory> findAllByOrderByDisplayOrderAscNameAsc();

    List<FoodCategory> findByActiveTrueOrderByDisplayOrderAscNameAsc();

    Optional<FoodCategory> findBySlug(String slug);

    boolean existsByNameIgnoreCase(String name);
}
