package com.itineraryledger.kabengosafaris.Tariff.Repositories;

import com.itineraryledger.kabengosafaris.Activity.ChargingBasis;
import com.itineraryledger.kabengosafaris.Tariff.Tariff;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * TariffRepository - Data access layer for Tariff entities
 *
 * Provides:
 * - Basic CRUD operations via JpaRepository
 * - Dynamic filtering via JpaSpecificationExecutor
 * - Custom query methods for business logic
 */
@Repository
public interface TariffRepository extends JpaRepository<Tariff, Long>, JpaSpecificationExecutor<Tariff> {

    // ========================
    // FIND BY NAME
    // ========================

    /**
     * Find tariff by exact name (case-insensitive)
     */
    Optional<Tariff> findByNameIgnoreCase(String name);

    /**
     * Find tariff by slug
     */
    Optional<Tariff> findBySlug(String slug);

    // ========================
    // EXISTENCE CHECKS
    // ========================

    /**
     * Check if tariff exists by name (case-insensitive)
     * Used for create validation
     */
    boolean existsByNameIgnoreCase(String name);

    /**
     * Check if tariff exists by name, excluding a specific ID
     * Used for update validation
     */
    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    /**
     * Check if tariff exists by slug
     */
    boolean existsBySlug(String slug);

    /**
     * Check if tariff exists by slug, excluding a specific ID
     */
    boolean existsBySlugAndIdNot(String slug, Long id);

    // ========================
    // FIND BY CRITERIA
    // ========================

    /**
     * Find all tariffs by charging basis
     */
    List<Tariff> findByChargingBasis(ChargingBasis chargingBasis);

    /**
     * Find all active tariffs
     */
    List<Tariff> findByIsActiveTrue();

    /**
     * Find all active tariffs by charging basis
     */
    List<Tariff> findByChargingBasisAndIsActiveTrue(ChargingBasis chargingBasis);

    /**
     * Find all system tariffs
     */
    List<Tariff> findByIsSystemTrue();

    /**
     * Find all non-system (user-created) tariffs
     */
    List<Tariff> findByIsSystemFalse();

    // ========================
    // COUNT METHODS
    // ========================

    /**
     * Count tariffs by charging basis
     */
    long countByChargingBasis(ChargingBasis chargingBasis);

    /**
     * Count active tariffs
     */
    long countByIsActiveTrue();

    /**
     * Count system tariffs
     */
    long countByIsSystemTrue();

    @Query("SELECT e.id FROM Tariff e WHERE e.id > :currentId ORDER BY e.id ASC LIMIT 1")
    Optional<Long> findNextId(@Param("currentId") Long currentId);

    @Query("SELECT e.id FROM Tariff e WHERE e.id < :currentId ORDER BY e.id DESC LIMIT 1")
    Optional<Long> findPreviousId(@Param("currentId") Long currentId);

    @Query("SELECT e.id FROM Tariff e ORDER BY e.id ASC LIMIT 1")
    Optional<Long> findFirstId();

    @Query("SELECT e.id FROM Tariff e ORDER BY e.id DESC LIMIT 1")
    Optional<Long> findLastId();
}
