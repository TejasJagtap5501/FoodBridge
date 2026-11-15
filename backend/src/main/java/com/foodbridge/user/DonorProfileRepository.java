package com.foodbridge.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DonorProfileRepository extends JpaRepository<DonorProfile, Long> {

    Optional<DonorProfile> findByUserId(Long userId);
}
