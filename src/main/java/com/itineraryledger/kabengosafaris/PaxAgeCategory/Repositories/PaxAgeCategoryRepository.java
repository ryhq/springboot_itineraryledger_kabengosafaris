package com.itineraryledger.kabengosafaris.PaxAgeCategory.Repositories;

import com.itineraryledger.kabengosafaris.PaxAgeCategory.PaxAgeCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * PaxAgeCategoryRepository - Data access layer for PaxAgeCategory entity
 */
@Repository
public interface PaxAgeCategoryRepository extends JpaRepository<PaxAgeCategory, Long>, JpaSpecificationExecutor<PaxAgeCategory> {

    /**
     * Find a category by name (case-insensitive)
     */
    Optional<PaxAgeCategory> findByNameIgnoreCase(String name);

    /**
     * Check if a category name already exists (case-insensitive)
     */
    boolean existsByNameIgnoreCase(String name);

    /**
     * Check if a category name already exists excluding a specific ID (for updates)
     */
    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    /**
     * Find unique categories based on category type
     * Returns one category per unique category type, sorted by name
     */
    @Query("""
        SELECT p FROM PaxAgeCategory p
        WHERE p.id IN (
            SELECT MIN(p2.id)
            FROM PaxAgeCategory p2
            WHERE p2.isActive = true
            GROUP BY p2.categoryType
        )
        ORDER BY p.name ASC
        """)
    List<PaxAgeCategory> findUniqueCategoriesByType();
}
