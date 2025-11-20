package com.itineraryledger.kabengosafaris.Permission;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.User.User;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * EndpointPermissionService - Manages endpoint-to-permission mappings
 *
 * This service handles all CRUD operations for endpoint permission configurations.
 * It provides dynamic URL-to-Permission mapping without requiring code changes.
 *
 * Key Responsibilities:
 * - Register/manage endpoint permission mappings
 * - Lookup endpoint permissions with caching
 * - Validate user access to endpoints
 * - Support both exact path and regex pattern matching
 * - Support multiple permission models (name-based and action-based)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EndpointPermissionService {

    private final EndpointPermissionRepository repository;

    // Cache for compiled regex patterns (for performance)
    private final Map<String, Pattern> patternCache = new HashMap<>();

    /**
     * Get endpoint permission mapping with caching
     * Tries exact match first, then pattern matching
     *
     * @param httpMethod HTTP method (GET, POST, PUT, DELETE, PATCH)
     * @param endpoint request endpoint path
     * @return Optional containing endpoint permission if found
     */
    @Cacheable(cacheNames = "endpointPermissions", key = "#httpMethod + ':' + #endpoint")
    public Optional<EndpointPermission> getEndpointPermission(String httpMethod, String endpoint) {
        log.debug("Looking up endpoint permission for: {} {}", httpMethod, endpoint);

        // Try exact match first (fast path)
        Optional<EndpointPermission> exactMatch = repository.findByHttpMethodAndEndpoint(httpMethod, endpoint);
        if (exactMatch.isPresent()) {
            log.debug("Found exact endpoint match: {} {}", httpMethod, endpoint);
            return exactMatch;
        }

        // Try pattern matching (slower but more flexible)
        List<EndpointPermission> patterns = repository.findByHttpMethod(httpMethod).stream()
            .filter(EndpointPermission::getRequiresPatternMatching)
            .filter(EndpointPermission::getActive)
            .collect(Collectors.toList());

        for (EndpointPermission pattern : patterns) {
            if (matchesPattern(endpoint, pattern.getEndpoint())) {
                log.debug("Found pattern match for: {} {} -> pattern: {}",
                    httpMethod, endpoint, pattern.getEndpoint());
                return Optional.of(pattern);
            }
        }

        log.debug("No endpoint permission found for: {} {}", httpMethod, endpoint);
        return Optional.empty();
    }

    /**
     * Check if endpoint path matches a regex pattern
     * Supports wildcard notation (/* becomes .*)
     *
     * @param path actual request path
     * @param pattern regex pattern or wildcard pattern
     * @return true if path matches pattern
     */
    private boolean matchesPattern(String path, String pattern) {
        try {
            // Convert wildcard to regex if needed
            String regexPattern = pattern.replace("*", ".*");

            // Get or compile pattern
            Pattern p = patternCache.computeIfAbsent(regexPattern, key -> {
                try {
                    return Pattern.compile(key);
                } catch (PatternSyntaxException e) {
                    log.warn("Invalid regex pattern: {}", key);
                    return null;
                }
            });

            if (p == null) {
                return false;
            }

            boolean matches = p.matcher(path).matches();
            log.debug("Pattern '{}' {} path '{}'", pattern, matches ? "matches" : "does not match", path);
            return matches;

        } catch (Exception e) {
            log.warn("Error matching pattern: {} against path: {}", pattern, path, e);
            return false;
        }
    }

    /**
     * Check if user can access an endpoint
     * Validates permissions, roles, and auth requirements
     *
     * @param user current user (null if not authenticated)
     * @param httpMethod HTTP method
     * @param endpoint request endpoint path
     * @return true if user is allowed to access endpoint
     */
    public boolean canUserAccessEndpoint(User user, String httpMethod, String endpoint) {
        Optional<EndpointPermission> permissionOpt = getEndpointPermission(httpMethod, endpoint);

        // If endpoint not in database, default behavior (can be configured)
        if (permissionOpt.isEmpty()) {
            log.debug("No endpoint mapping found for {} {}. Allowing by default.", httpMethod, endpoint);
            return true;  // Default: allow if not explicitly configured
        }

        EndpointPermission permission = permissionOpt.get();

        // Check if endpoint is disabled
        if (!permission.getActive()) {
            log.warn("Access denied: endpoint disabled: {} {}", httpMethod, endpoint);
            return false;
        }

        // If endpoint is public, allow everyone
        if (!permission.getRequiresAuth()) {
            log.debug("Public endpoint: {} {}", httpMethod, endpoint);
            return true;
        }

        // User must be authenticated for protected endpoints
        if (user == null) {
            log.warn("Access denied: user not authenticated for {} {}", httpMethod, endpoint);
            return false;
        }

        // Check role-based access (if configured)
        if (permission.requiresRole()) {
            List<String> allowedRoles = permission.getAllowedRolesList();
            boolean hasRole = user.getRoles().stream()
                .filter(r -> r.getActive() && allowedRoles.contains(r.getName()))
                .findAny()
                .isPresent();

            if (!hasRole) {
                log.warn("Access denied: user {} does not have required role for {} {}",
                    user.getUsername(), httpMethod, endpoint);
                return false;
            }

            log.debug("User {} has required role for {} {}", user.getUsername(), httpMethod, endpoint);
            return true;
        }

        // Check permission-based access (Model 1: specific permission name)
        if (permission.getRequiredPermissionName() != null) {
            boolean hasPermission = user.hasPermission(permission.getRequiredPermissionName());

            if (!hasPermission) {
                log.warn("Access denied: user {} does not have permission '{}' for {} {}",
                    user.getUsername(), permission.getRequiredPermissionName(), httpMethod, endpoint);
                return false;
            }

            log.debug("User {} has permission '{}' for {} {}",
                user.getUsername(), permission.getRequiredPermissionName(), httpMethod, endpoint);
            return true;
        }

        // Check permission-based access (Model 2: action + resource)
        if (permission.getActionCode() != null && permission.getResourceType() != null) {
            boolean hasPermission = user.hasPermission(permission.getActionCode(), permission.getResourceType());

            if (!hasPermission) {
                log.warn("Access denied: user {} cannot '{}' on '{}' for {} {}",
                    user.getUsername(), permission.getActionCode(), permission.getResourceType(),
                    httpMethod, endpoint);
                return false;
            }

            log.debug("User {} can '{}' on '{}' for {} {}",
                user.getUsername(), permission.getActionCode(), permission.getResourceType(),
                httpMethod, endpoint);
            return true;
        }

        // If authenticated but no permission specified, allow
        log.debug("User {} authenticated and no specific permission required for {} {}",
            user.getUsername(), httpMethod, endpoint);
        return true;
    }

    /**
     * Register a new endpoint permission mapping at runtime
     *
     * @param httpMethod HTTP method
     * @param endpoint endpoint path
     * @param requiredPermissionName required permission name (or null)
     * @return created EndpointPermission
     */
    @CacheEvict(cacheNames = "endpointPermissions", allEntries = true)
    @Transactional
    public EndpointPermission registerEndpoint(String httpMethod, String endpoint,
                                              String requiredPermissionName) {
        return registerEndpoint(httpMethod, endpoint, requiredPermissionName, null, null, false);
    }

    /**
     * Register a new endpoint permission mapping with action+resource
     *
     * @param httpMethod HTTP method
     * @param endpoint endpoint path
     * @param actionCode action code (or null)
     * @param resourceType resource type (or null)
     * @return created EndpointPermission
     */
    @CacheEvict(cacheNames = "endpointPermissions", allEntries = true)
    @Transactional
    public EndpointPermission registerEndpointByAction(String httpMethod, String endpoint,
                                                       String actionCode, String resourceType) {
        return registerEndpoint(httpMethod, endpoint, null, actionCode, resourceType, false);
    }

    /**
     * Register public endpoint (no authentication required)
     *
     * @param httpMethod HTTP method
     * @param endpoint endpoint path
     * @return created EndpointPermission
     */
    @CacheEvict(cacheNames = "endpointPermissions", allEntries = true)
    @Transactional
    public EndpointPermission registerPublicEndpoint(String httpMethod, String endpoint) {
        EndpointPermission mapping = EndpointPermission.builder()
            .httpMethod(httpMethod)
            .endpoint(endpoint)
            .requiresAuth(false)
            .active(true)
            .build();

        EndpointPermission saved = repository.save(mapping);
        log.info("Registered public endpoint: {} {}", httpMethod, endpoint);
        return saved;
    }

    /**
     * Internal method for registering endpoints
     */
    @Transactional
    private EndpointPermission registerEndpoint(String httpMethod, String endpoint,
                                               String requiredPermissionName,
                                               String actionCode, String resourceType,
                                               boolean usePatternMatching) {
        EndpointPermission mapping = EndpointPermission.builder()
            .httpMethod(httpMethod.toUpperCase())
            .endpoint(endpoint)
            .requiredPermissionName(requiredPermissionName)
            .actionCode(actionCode)
            .resourceType(resourceType)
            .requiresAuth(requiredPermissionName != null || actionCode != null || resourceType != null)
            .requiresPatternMatching(usePatternMatching)
            .active(true)
            .build();

        EndpointPermission saved = repository.save(mapping);
        log.info("Registered endpoint: {} {} -> {}", httpMethod, endpoint, mapping);
        return saved;
    }

    /**
     * Update endpoint permission mapping
     *
     * @param id endpoint permission ID
     * @param requiredPermissionName new required permission
     * @return updated EndpointPermission
     */
    @CacheEvict(cacheNames = "endpointPermissions", allEntries = true)
    @Transactional
    public EndpointPermission updateEndpointPermission(Long id, String requiredPermissionName) {
        EndpointPermission mapping = repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Endpoint permission not found: " + id));

        mapping.setRequiredPermissionName(requiredPermissionName);
        mapping.setActionCode(null);
        mapping.setResourceType(null);
        mapping.setAllowedRoles(null);

        EndpointPermission updated = repository.save(mapping);
        log.info("Updated endpoint permission: {} -> {}", id, requiredPermissionName);
        return updated;
    }

    /**
     * Enable/disable endpoint without deleting
     *
     * @param id endpoint permission ID
     * @param active true to enable, false to disable
     * @return updated EndpointPermission
     */
    @CacheEvict(cacheNames = "endpointPermissions", allEntries = true)
    @Transactional
    public EndpointPermission setEndpointActive(Long id, Boolean active) {
        EndpointPermission mapping = repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Endpoint permission not found: " + id));

        mapping.setActive(active);
        EndpointPermission updated = repository.save(mapping);
        log.info("Set endpoint {} to: {}", id, active ? "ACTIVE" : "DISABLED");
        return updated;
    }

    /**
     * Get all endpoint permissions
     *
     * @return list of all active endpoint permissions
     */
    public List<EndpointPermission> getAllEndpoints() {
        return repository.findByActiveTrue();
    }

    /**
     * Get all public endpoints
     *
     * @return list of public endpoint permissions
     */
    public List<EndpointPermission> getPublicEndpoints() {
        return repository.findByRequiresAuthFalse();
    }

    /**
     * Get endpoints by permission name
     *
     * @param permissionName permission name
     * @return list of endpoints requiring this permission
     */
    public List<EndpointPermission> getEndpointsByPermission(String permissionName) {
        return repository.findByRequiredPermissionName(permissionName);
    }

    /**
     * Get endpoints by resource type
     *
     * @param resourceType resource type
     * @return list of endpoints for this resource
     */
    public List<EndpointPermission> getEndpointsByResource(String resourceType) {
        return repository.findByResourceType(resourceType);
    }

    /**
     * Clear pattern cache (useful after adding pattern-based endpoints)
     */
    public void clearPatternCache() {
        patternCache.clear();
        log.debug("Cleared endpoint permission pattern cache");
    }

    /**
     * Clear all caches and reload from database
     */
    @CacheEvict(cacheNames = "endpointPermissions", allEntries = true)
    public void clearAllCaches() {
        clearPatternCache();
        log.info("Cleared all endpoint permission caches");
    }
}
