package com.itineraryledger.kabengosafaris.Permission;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * PermissionActionTypeRepository - Data access for action type definitions
 *
 * Provides query methods for retrieving action types from the database.
 * Used by PermissionActionService and PermissionCheckAspect for dynamic permission evaluation.
 * Supports both system actions (fixed) and custom actions (user-defined).
 */
@Repository
public interface PermissionActionTypeRepository extends JpaRepository<PermissionActionType, Long> {

    /**
     * Find action type by code
     *
     * @param code unique action code (e.g., "create", "read", "update")
     * @return Optional containing the action type if found
     */
    Optional<PermissionActionType> findByCode(String code);

    /**
     * Check if action type exists by code
     *
     * @param code action code
     * @return true if exists
     */
    boolean existsByCode(String code);

    /**
     * Get all active action types
     * Used by caching service to load all available actions
     *
     * @return list of active action types
     */
    List<PermissionActionType> findByActiveTrue();

    /**
     * Get all inactive action types
     *
     * @return list of inactive action types
     */
    List<PermissionActionType> findByActiveFalse();

    /**
     * Get all system action types (protected from deletion)
     *
     * @return list of system action types
     */
    List<PermissionActionType> findByIsSystemTrue();

    /**
     * Get all custom (non-system) action types
     *
     * @return list of custom action types
     */
    List<PermissionActionType> findByIsSystemFalse();

    /**
     * Get all active system action types
     * Used during initialization to verify system actions exist
     *
     * @return list of active system actions
     */
    @Query("SELECT a FROM PermissionActionType a WHERE a.active = true AND a.isSystem = true")
    List<PermissionActionType> findActiveSystemActions();

    /**
     * Search action types by code or description
     * Supports fuzzy search for discovery
     *
     * @param searchTerm search term to match against code or description
     * @return list of matching action types
     */
    @Query("SELECT a FROM PermissionActionType a WHERE " +
           "LOWER(a.code) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(a.description) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<PermissionActionType> searchByCodeOrDescription(@Param("searchTerm") String searchTerm);

    /**
     * Get count of active/inactive action types
     *
     * @param active status to count
     * @return count of actions with specified status
     */
    long countByActive(Boolean active);

    /**
     * Get count of system/custom action types
     *
     * @param isSystem true for system, false for custom
     * @return count of actions with specified type
     */
    long countByIsSystem(Boolean isSystem);
}
