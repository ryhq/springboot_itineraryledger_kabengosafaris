package com.itineraryledger.kabengosafaris.Accommodation.Repositories;

import com.itineraryledger.kabengosafaris.Accommodation.Entities.AccommodationEmail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AccommodationEmailRepository extends JpaRepository<AccommodationEmail, Long>, JpaSpecificationExecutor<AccommodationEmail> {

    /**
     * Find email by exact email address
     */
    Optional<AccommodationEmail> findByEmail(String email);

    /**
     * Check if email exists
     */
    boolean existsByEmail(String email);

    /**
     * Mark all emails for an accommodation as non-primary
     */
    @Modifying
    @Query("UPDATE AccommodationEmail e SET e.isPrimary = false WHERE e.accommodation.id = :accommodationId")
    void markAllAsNonPrimaryForAccommodation(@Param("accommodationId") Long accommodationId);

    /**
     * Mark all emails for an accommodation except one as non-primary
     */
    @Modifying
    @Query("UPDATE AccommodationEmail e SET e.isPrimary = false WHERE e.accommodation.id = :accommodationId AND e.id != :excludeEmailId")
    void markAllAsNonPrimaryExcept(@Param("accommodationId") Long accommodationId, @Param("excludeEmailId") Long excludeEmailId);

    // ========================
    // NAVIGATION QUERIES (circular next/previous)
    // ========================

    @Query("SELECT e.id FROM AccommodationEmail e WHERE e.id > :currentId ORDER BY e.id ASC LIMIT 1")
    Optional<Long> findNextId(@Param("currentId") Long currentId);

    @Query("SELECT e.id FROM AccommodationEmail e WHERE e.id < :currentId ORDER BY e.id DESC LIMIT 1")
    Optional<Long> findPreviousId(@Param("currentId") Long currentId);

    @Query("SELECT e.id FROM AccommodationEmail e ORDER BY e.id ASC LIMIT 1")
    Optional<Long> findFirstId();

    @Query("SELECT e.id FROM AccommodationEmail e ORDER BY e.id DESC LIMIT 1")
    Optional<Long> findLastId();
}
