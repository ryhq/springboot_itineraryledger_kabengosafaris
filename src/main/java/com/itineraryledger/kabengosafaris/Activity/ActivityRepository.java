package com.itineraryledger.kabengosafaris.Activity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * ActivityRepository - Data access layer for Activity entity
 *
 * Extends JpaRepository for basic CRUD operations
 * Extends JpaSpecificationExecutor for dynamic query capabilities
 */
@Repository
public interface ActivityRepository extends JpaRepository<Activity, Long>, JpaSpecificationExecutor<Activity> {

    /**
     * Find activity by name
     *
     * @param name The activity name
     * @return Optional containing the activity if found
     */
    Optional<Activity> findByName(String name);

    /**
     * Find activity by slug
     *
     * @param slug The activity slug
     * @return Optional containing the activity if found
     */
    Optional<Activity> findBySlug(String slug);

    /**
     * Check if activity exists by name
     *
     * @param name The activity name
     * @return true if exists, false otherwise
     */
    boolean existsByName(String name);

    /**
     * Check if activity exists by slug
     *
     * @param slug The activity slug
     * @return true if exists, false otherwise
     */
    boolean existsBySlug(String slug);

    // ========================
    // NAVIGATION QUERIES (circular next/previous)
    // ========================

    @Query("SELECT e.id FROM Activity e WHERE e.id > :currentId ORDER BY e.id ASC LIMIT 1")
    Optional<Long> findNextId(@Param("currentId") Long currentId);

    @Query("SELECT e.id FROM Activity e WHERE e.id < :currentId ORDER BY e.id DESC LIMIT 1")
    Optional<Long> findPreviousId(@Param("currentId") Long currentId);

    @Query("SELECT e.id FROM Activity e ORDER BY e.id ASC LIMIT 1")
    Optional<Long> findFirstId();

    @Query("SELECT e.id FROM Activity e ORDER BY e.id DESC LIMIT 1")
    Optional<Long> findLastId();
}
