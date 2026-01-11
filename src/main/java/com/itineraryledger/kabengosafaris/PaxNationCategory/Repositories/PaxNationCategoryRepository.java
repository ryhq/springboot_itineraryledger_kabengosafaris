package com.itineraryledger.kabengosafaris.PaxNationCategory.Repositories;

import com.itineraryledger.kabengosafaris.PaxNationCategory.PaxNationCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for PaxNationCategory entity
 *
 * Extends JpaSpecificationExecutor for dynamic query building via Specifications
 */
@Repository
public interface PaxNationCategoryRepository extends JpaRepository<PaxNationCategory, Long>,
        JpaSpecificationExecutor<PaxNationCategory> {

    /**
     * Find category by name (case-insensitive)
     */
    Optional<PaxNationCategory> findByNameIgnoreCase(String name);

    /**
     * Check if category name exists (case-insensitive)
     */
    boolean existsByNameIgnoreCase(String name);

    /**
     * Check if category name exists excluding a specific ID (for updates)
     */
    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    /**
     * Check if priority factor exists
     */
    boolean existsByPriorityFactor(Integer priorityFactor);

    /**
     * Check if priority factor exists excluding a specific ID (for updates)
     */
    boolean existsByPriorityFactorAndIdNot(Integer priorityFactor, Long id);

    /**
     * Find category by priority factor
     */
    Optional<PaxNationCategory> findByPriorityFactor(Integer priorityFactor);
}
