package com.itineraryledger.kabengosafaris.Park;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * ParkRepository - Data access layer for Park entity
 *
 * Extends JpaSpecificationExecutor to support dynamic filtering using Specifications
 */
@Repository
public interface ParkRepository extends JpaRepository<Park, Long>, JpaSpecificationExecutor<Park> {

    /**
     * Find a park by its name
     *
     * @param name The park name
     * @return Optional containing the park if found
     */
    Optional<Park> findByName(String name);

    /**
     * Find a park by its slug
     *
     * @param slug The URL-friendly slug
     * @return Optional containing the park if found
     */
    Optional<Park> findBySlug(String slug);

    /**
     * Check if a park exists by name
     *
     * @param name The park name
     * @return true if exists, false otherwise
     */
    boolean existsByName(String name);

    /**
     * Check if a park exists by slug
     *
     * @param slug The URL-friendly slug
     * @return true if exists, false otherwise
     */
    boolean existsBySlug(String slug);
}
