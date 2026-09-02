package com.itineraryledger.kabengosafaris.Accommodation.Repositories;

import com.itineraryledger.kabengosafaris.Accommodation.Entities.AccommodationPhone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AccommodationPhoneRepository extends JpaRepository<AccommodationPhone, Long>, JpaSpecificationExecutor<AccommodationPhone> {

    /**
     * Find phone by exact phone number
     */
    /** Every number on one property, so the data transfer can carry them with it. */
    java.util.List<AccommodationPhone> findByAccommodationId(Long accommodationId);

    Optional<AccommodationPhone> findByPhoneNumber(String phoneNumber);

    /**
     * Check if phone number exists
     */
    boolean existsByPhoneNumber(String phoneNumber);

    /** Is this number already on THIS accommodation? See the note on the email repository. */
    boolean existsByAccommodationIdAndPhoneNumber(Long accommodationId, String phoneNumber);

    boolean existsByAccommodationIdAndPhoneNumberAndIdNot(Long accommodationId, String phoneNumber, Long id);

    /**
     * Mark all phones for an accommodation as non-primary
     */
    @Modifying
    @Query("UPDATE AccommodationPhone p SET p.isPrimary = false WHERE p.accommodation.id = :accommodationId")
    void markAllAsNonPrimaryForAccommodation(@Param("accommodationId") Long accommodationId);

    /**
     * Mark all phones for an accommodation except one as non-primary
     */
    @Modifying
    @Query("UPDATE AccommodationPhone p SET p.isPrimary = false WHERE p.accommodation.id = :accommodationId AND p.id != :excludePhoneId")
    void markAllAsNonPrimaryExcept(@Param("accommodationId") Long accommodationId, @Param("excludePhoneId") Long excludePhoneId);

    // ========================
    // NAVIGATION QUERIES (circular next/previous)
    // ========================

    @Query("SELECT p.id FROM AccommodationPhone p WHERE p.id > :currentId ORDER BY p.id ASC LIMIT 1")
    Optional<Long> findNextId(@Param("currentId") Long currentId);

    @Query("SELECT p.id FROM AccommodationPhone p WHERE p.id < :currentId ORDER BY p.id DESC LIMIT 1")
    Optional<Long> findPreviousId(@Param("currentId") Long currentId);

    @Query("SELECT p.id FROM AccommodationPhone p ORDER BY p.id ASC LIMIT 1")
    Optional<Long> findFirstId();

    @Query("SELECT p.id FROM AccommodationPhone p ORDER BY p.id DESC LIMIT 1")
    Optional<Long> findLastId();

    // ========================
    // SCOPED NAVIGATION QUERIES (parent-scoped next/previous)
    // ========================

    @Query("SELECT p.id FROM AccommodationPhone p WHERE p.id > :currentId AND p.accommodation.id = :parentId ORDER BY p.id ASC LIMIT 1")
    Optional<Long> findNextIdByParent(@Param("currentId") Long currentId, @Param("parentId") Long parentId);

    @Query("SELECT p.id FROM AccommodationPhone p WHERE p.id < :currentId AND p.accommodation.id = :parentId ORDER BY p.id DESC LIMIT 1")
    Optional<Long> findPreviousIdByParent(@Param("currentId") Long currentId, @Param("parentId") Long parentId);

    @Query("SELECT p.id FROM AccommodationPhone p WHERE p.accommodation.id = :parentId ORDER BY p.id ASC LIMIT 1")
    Optional<Long> findFirstIdByParent(@Param("parentId") Long parentId);

    @Query("SELECT p.id FROM AccommodationPhone p WHERE p.accommodation.id = :parentId ORDER BY p.id DESC LIMIT 1")
    Optional<Long> findLastIdByParent(@Param("parentId") Long parentId);
}
