package com.itineraryledger.kabengosafaris.Permission;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * EndpointPermissionRepository - Data access for endpoint permission mappings
 *
 * Provides methods to retrieve endpoint permission configurations from database.
 * Used by EndpointPermissionService and DynamicPermissionFilter for runtime
 * permission checking based on URL patterns.
 */
@Repository
public interface EndpointPermissionRepository extends JpaRepository<EndpointPermission, Long> {

    /**
     * Find exact endpoint permission mapping by HTTP method and endpoint path
     *
     * @param httpMethod HTTP method (GET, POST, PUT, DELETE, PATCH, etc.)
     * @param endpoint exact endpoint path
     * @return Optional containing the endpoint permission if found
     */
    Optional<EndpointPermission> findByHttpMethodAndEndpoint(String httpMethod, String endpoint);

    /**
     * Find all active endpoint permission mappings
     *
     * @return list of active endpoint permission mappings
     */
    List<EndpointPermission> findByActiveTrue();

    /**
     * Find all inactive endpoint permission mappings
     *
     * @return list of inactive endpoint permission mappings
     */
    List<EndpointPermission> findByActiveFalse();

    /**
     * Find endpoint permissions by HTTP method
     *
     * @param httpMethod HTTP method
     * @return list of endpoint permissions for the method
     */
    List<EndpointPermission> findByHttpMethod(String httpMethod);

    /**
     * Find endpoint permissions by partial endpoint path (LIKE match)
     * Useful for finding patterns
     *
     * @param endpoint endpoint path or pattern
     * @return list of matching endpoint permissions
     */
    List<EndpointPermission> findByEndpointLike(String endpoint);

    /**
     * Find all public endpoints (no authentication required)
     *
     * @return list of public endpoint permissions
     */
    List<EndpointPermission> findByRequiresAuthFalse();

    /**
     * Find all pattern-based endpoint permissions
     * Useful for loading pattern matchers at startup
     *
     * @return list of pattern-based endpoint permissions
     */
    List<EndpointPermission> findByRequiresPatternMatchingTrue();

    /**
     * Find endpoints by required permission name
     *
     * @param permissionName permission name
     * @return list of endpoints requiring this permission
     */
    List<EndpointPermission> findByRequiredPermissionName(String permissionName);

    /**
     * Find endpoints by action code
     *
     * @param actionCode action code
     * @return list of endpoints requiring this action
     */
    List<EndpointPermission> findByActionCode(String actionCode);

    /**
     * Find endpoints by resource type
     *
     * @param resourceType resource type
     * @return list of endpoints for this resource
     */
    List<EndpointPermission> findByResourceType(String resourceType);

    /**
     * Find endpoints by allowed roles
     * Uses LIKE to match comma-separated role names
     *
     * @param roleName role name
     * @return list of endpoints allowing this role
     */
    @Query("SELECT ep FROM EndpointPermission ep WHERE ep.allowedRoles LIKE CONCAT('%', :roleName, '%')")
    List<EndpointPermission> findByAllowedRole(@Param("roleName") String roleName);

    /**
     * Check if endpoint mapping exists
     *
     * @param httpMethod HTTP method
     * @param endpoint endpoint path
     * @return true if mapping exists
     */
    boolean existsByHttpMethodAndEndpoint(String httpMethod, String endpoint);

    /**
     * Check if endpoint is public
     *
     * @param httpMethod HTTP method
     * @param endpoint endpoint path
     * @return true if endpoint is public
     */
    @Query("SELECT (COUNT(ep) > 0) FROM EndpointPermission ep WHERE " +
           "ep.httpMethod = :httpMethod AND ep.endpoint = :endpoint AND ep.requiresAuth = false")
    boolean isEndpointPublic(@Param("httpMethod") String httpMethod, @Param("endpoint") String endpoint);

    /**
     * Get all endpoints for a resource (useful for managing permissions by resource)
     *
     * @param httpMethod HTTP method
     * @param resourceType resource type
     * @return list of endpoints for resource
     */
    @Query("SELECT ep FROM EndpointPermission ep WHERE " +
           "ep.httpMethod = :httpMethod AND ep.resourceType = :resourceType AND ep.active = true")
    List<EndpointPermission> findByResourceTypeAndMethod(@Param("httpMethod") String httpMethod,
                                                         @Param("resourceType") String resourceType);

    /**
     * Get count of active endpoints
     *
     * @return count of active endpoint mappings
     */
    long countByActiveTrue();

    /**
     * Get count of public endpoints
     *
     * @return count of public endpoints
     */
    long countByRequiresAuthFalse();
}
