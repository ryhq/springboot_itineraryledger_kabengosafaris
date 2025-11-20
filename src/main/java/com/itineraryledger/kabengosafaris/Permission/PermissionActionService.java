package com.itineraryledger.kabengosafaris.Permission;

import java.util.List;
import java.util.Optional;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * PermissionActionService - Manages permission action type definitions
 *
 * This service handles all CRUD operations for permission action types (CREATE, READ, UPDATE, DELETE, etc.).
 * It provides caching for performance optimization and ensures system actions cannot be deleted.
 *
 * Key Responsibilities:
 * - Retrieve action types from database with caching
 * - Create custom action types at runtime
 * - Deactivate (soft delete) action types
 * - Initialize system action types on application startup
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PermissionActionService {

    private final PermissionActionTypeRepository repository;

    /**
     * Get all active permission action types with caching
     * Cached to improve performance for frequent permission checks
     *
     * @return list of all active action types
     */
    @Cacheable(cacheNames = "actionTypes", key = "'all-active'")
    public List<PermissionActionType> getAllActiveActions() {
        log.debug("Loading all active action types from database");
        return repository.findByActiveTrue();
    }

    /**
     * Get action type by code (no caching for single lookups to ensure freshness)
     *
     * @param code unique action code (e.g., "create", "read", "update")
     * @return Optional containing the action type if found
     */
    public Optional<PermissionActionType> getActionByCode(String code) {
        return repository.findByCode(code);
    }

    /**
     * Check if action code exists
     *
     * @param code action code to check
     * @return true if action code exists
     */
    public boolean actionExists(String code) {
        return repository.existsByCode(code);
    }

    /**
     * Create a new custom action type (system actions cannot be created via this method)
     * Validates that the action code doesn't already exist
     *
     * @param code unique action code (e.g., "approve", "reject", "review")
     * @param description human-readable description
     * @return the created PermissionActionType
     * @throws IllegalArgumentException if code already exists
     */
    @CacheEvict(cacheNames = "actionTypes", allEntries = true)
    @Transactional
    public PermissionActionType createCustomAction(String code, String description) {
        if (repository.existsByCode(code)) {
            log.warn("Attempted to create action type with existing code: {}", code);
            throw new IllegalArgumentException("Action code already exists: " + code);
        }

        PermissionActionType action = PermissionActionType.builder()
            .code(code.toLowerCase())
            .description(description)
            .active(true)
            .isSystem(false)  // Custom actions are not system actions
            .build();

        PermissionActionType saved = repository.save(action);
        log.info("Created custom action type: code={}, description={}", code, description);
        return saved;
    }

    /**
     * Deactivate an action type (soft delete)
     * System actions cannot be deactivated
     *
     * @param id action type ID to deactivate
     * @return the deactivated PermissionActionType
     * @throws IllegalArgumentException if trying to deactivate a system action
     */
    @CacheEvict(cacheNames = "actionTypes", allEntries = true)
    @Transactional
    public PermissionActionType deactivateAction(Long id) {
        PermissionActionType action = repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Action type not found with ID: " + id));

        if (action.getIsSystem()) {
            log.warn("Attempted to deactivate system action: {}", action.getCode());
            throw new IllegalArgumentException("Cannot deactivate system action types");
        }

        action.setActive(false);
        PermissionActionType updated = repository.save(action);
        log.info("Deactivated action type: code={}", action.getCode());
        return updated;
    }

    /**
     * Initialize default system action types on application startup
     * Only runs if no action types exist in database
     *
     * This ensures that the system always has the core action types:
     * CREATE, READ, UPDATE, DELETE, EXECUTE, SUBMIT, AMEND, CANCEL, EXPORT, PRINT
     */
    @Transactional
    public void initializeSystemActions() {
        long count = repository.countByActive(true);
        if (count > 0) {
            log.debug("Action types already initialized, skipping initialization");
            return;
        }

        log.info("Initializing system action types...");

        String[][] defaultActions = {
            {"create", "Create new records"},
            {"read", "View and read records"},
            {"update", "Edit and update records"},
            {"delete", "Delete records"},
            {"execute", "Execute actions and workflows"},
            {"submit", "Submit documents for approval"},
            {"amend", "Amend submitted documents"},
            {"cancel", "Cancel submitted documents"},
            {"export", "Export data to external formats"},
            {"print", "Print documents"}
        };

        for (String[] actionData : defaultActions) {
            String code = actionData[0];
            String description = actionData[1];

            if (!repository.existsByCode(code)) {
                try {
                    PermissionActionType action = PermissionActionType.builder()
                        .code(code)
                        .description(description)
                        .active(true)
                        .isSystem(true)
                        .build();
                    repository.save(action);
                    log.debug("Created system action type: {}", code);
                } catch (DataIntegrityViolationException e) {
                    log.debug("Action type already exists: {}", code);
                }
            }
        }

        log.info("System action types initialization complete");
    }

    /**
     * Get all active system action types
     * Used for validation and listing
     *
     * @return list of active system action types
     */
    public List<PermissionActionType> getActiveSystemActions() {
        return repository.findActiveSystemActions();
    }

    /**
     * Search for action types by code or description
     * Used for discovery and autocomplete
     *
     * @param searchTerm search term
     * @return list of matching action types
     */
    public List<PermissionActionType> searchActions(String searchTerm) {
        return repository.searchByCodeOrDescription(searchTerm);
    }

    /**
     * Get count of system action types
     *
     * @return count of system actions
     */
    public long countSystemActions() {
        return repository.countByIsSystem(true);
    }

    /**
     * Clear the action types cache
     * Useful when action types are modified externally
     */
    @CacheEvict(cacheNames = "actionTypes", allEntries = true)
    public void clearCache() {
        log.debug("Action types cache cleared");
    }
}
