package com.itineraryledger.kabengosafaris.Accommodation.Repositories;

import com.itineraryledger.kabengosafaris.Accommodation.Entities.Accommodation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AccommodationRepository extends JpaRepository<Accommodation, Long>, JpaSpecificationExecutor<Accommodation> {

    /**
     * Find accommodation by exact name
     */
    Optional<Accommodation> findByName(String name);

    /**
     * Find accommodation by slug
     */
    Optional<Accommodation> findBySlug(String slug);

    /**
     * Check if accommodation exists by name
     */
    boolean existsByName(String name);

    /**
     * Check if accommodation exists by slug
     */
    boolean existsBySlug(String slug);

    // ========================
    // NAVIGATION QUERIES (circular next/previous)
    // ========================

    @Query("SELECT e.id FROM Accommodation e WHERE e.id > :currentId ORDER BY e.id ASC LIMIT 1")
    Optional<Long> findNextId(@Param("currentId") Long currentId);

    @Query("SELECT e.id FROM Accommodation e WHERE e.id < :currentId ORDER BY e.id DESC LIMIT 1")
    Optional<Long> findPreviousId(@Param("currentId") Long currentId);

    @Query("SELECT e.id FROM Accommodation e ORDER BY e.id ASC LIMIT 1")
    Optional<Long> findFirstId();

    @Query("SELECT e.id FROM Accommodation e ORDER BY e.id DESC LIMIT 1")
    Optional<Long> findLastId();

    /** Properties pointing at this vendor — checked before a vendor is deleted. */
    long countByVendorId(Long vendorId);
}
