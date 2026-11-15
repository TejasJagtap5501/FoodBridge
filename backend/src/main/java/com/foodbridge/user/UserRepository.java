package com.foodbridge.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    java.util.List<User> findAllByRoleAndStatus(Role role, UserStatus status);

    long countByRole(Role role);

    long countByRoleAndStatus(Role role, UserStatus status);

    long countByStatus(UserStatus status);

    @Query("""
            SELECT u FROM User u
            WHERE (:role IS NULL OR u.role = :role)
              AND (:status IS NULL OR u.status = :status)
              AND (:search IS NULL
                   OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :search, '%')))
            """)
    Page<User> search(@Param("role") Role role,
                      @Param("status") UserStatus status,
                      @Param("search") String search,
                      Pageable pageable);
}
