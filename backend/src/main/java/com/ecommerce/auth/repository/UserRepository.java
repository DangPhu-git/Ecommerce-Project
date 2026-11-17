package com.ecommerce.auth.repository;

import com.ecommerce.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA Repository for User entity.
 * Provides case-insensitive lookups and soft-delete filtering.
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Finds a user by email ignoring case.
     *
     * @param email user email
     * @return Optional containing the User if found
     */
    Optional<User> findByEmailIgnoreCase(String email);

    /**
     * Finds an active (not soft-deleted) user by email ignoring case.
     *
     * @param email user email
     * @return Optional containing the active User if found
     */
    @Query("SELECT u FROM User u WHERE LOWER(u.email) = LOWER(:email) AND u.deletedAt IS NULL")
    Optional<User> findActiveByEmailIgnoreCase(@Param("email") String email);

    /**
     * Equivalent to findActiveByEmailIgnoreCase using Spring Data method convention.
     */
    Optional<User> findByEmailIgnoreCaseAndDeletedAtIsNull(String email);

    /**
     * Checks if a user exists with the given email (case-insensitive).
     */
    boolean existsByEmailIgnoreCase(String email);

    /**
     * Checks if an active user exists with the given email (case-insensitive).
     */
    boolean existsByEmailIgnoreCaseAndDeletedAtIsNull(String email);
}
