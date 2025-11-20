package com.itineraryledger.kabengosafaris.Permission;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * EndpointPermissionController - REST API for managing endpoint permissions
 *
 * Allows administrators to configure URL-to-Permission mappings at runtime
 * without code changes.
 *
 * Base path: /api/admin/endpoint-permissions
 */
@RestController
@RequestMapping("/api/admin/endpoint-permissions")
@RequiredArgsConstructor
@Slf4j
public class EndpointPermissionController {

    private final EndpointPermissionService endpointPermissionService;

    /**
     * Register a new endpoint with a specific permission requirement
     *
     * POST /api/admin/endpoint-permissions
     * {
     *   "httpMethod": "POST",
     *   "endpoint": "/api/parks",
     *   "requiredPermissionName": "create_park",
     *   "description": "Create new parks"
     * }
     */
    @PostMapping
    public ResponseEntity<EndpointPermission> registerEndpoint(
            @RequestBody EndpointPermissionDTO dto) {

        log.info("Registering endpoint: {} {}", dto.getHttpMethod(), dto.getEndpoint());

        EndpointPermission permission;

        if (dto.getRequiredPermissionName() != null && !dto.getRequiredPermissionName().isEmpty()) {
            // Register with specific permission name
            permission = endpointPermissionService.registerEndpoint(
                dto.getHttpMethod(),
                dto.getEndpoint(),
                dto.getRequiredPermissionName()
            );
        } else if (dto.getActionCode() != null && dto.getResourceType() != null) {
            // Register with action + resource
            permission = endpointPermissionService.registerEndpointByAction(
                dto.getHttpMethod(),
                dto.getEndpoint(),
                dto.getActionCode(),
                dto.getResourceType()
            );
        } else {
            // Register as public endpoint
            permission = endpointPermissionService.registerPublicEndpoint(
                dto.getHttpMethod(),
                dto.getEndpoint()
            );
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(permission);
    }

    /**
     * Get all endpoint permissions
     *
     * GET /api/admin/endpoint-permissions
     */
    @GetMapping
    public ResponseEntity<List<EndpointPermission>> getAllEndpoints() {
        List<EndpointPermission> endpoints = endpointPermissionService.getAllEndpoints();
        return ResponseEntity.ok(endpoints);
    }

    /**
     * Get public endpoints (no authentication required)
     *
     * GET /api/admin/endpoint-permissions/public
     */
    @GetMapping("/public")
    public ResponseEntity<List<EndpointPermission>> getPublicEndpoints() {
        List<EndpointPermission> endpoints = endpointPermissionService.getPublicEndpoints();
        return ResponseEntity.ok(endpoints);
    }

    /**
     * Get endpoints by permission name
     *
     * GET /api/admin/endpoint-permissions/by-permission?permission=create_park
     */
    @GetMapping("/by-permission")
    public ResponseEntity<List<EndpointPermission>> getEndpointsByPermission(
            @RequestParam String permission) {

        List<EndpointPermission> endpoints = endpointPermissionService.getEndpointsByPermission(permission);
        return ResponseEntity.ok(endpoints);
    }

    /**
     * Get endpoints by resource type
     *
     * GET /api/admin/endpoint-permissions/by-resource?resource=Park
     */
    @GetMapping("/by-resource")
    public ResponseEntity<List<EndpointPermission>> getEndpointsByResource(
            @RequestParam String resource) {

        List<EndpointPermission> endpoints = endpointPermissionService.getEndpointsByResource(resource);
        return ResponseEntity.ok(endpoints);
    }

    /**
     * Get a specific endpoint permission by ID
     *
     * GET /api/admin/endpoint-permissions/1
     */
    @GetMapping("/{id}")
    public ResponseEntity<EndpointPermission> getEndpointPermission(@PathVariable Long id) {
        // This would need a findById method in service
        return ResponseEntity.notFound().build();
    }

    /**
     * Update endpoint permission mapping
     *
     * PUT /api/admin/endpoint-permissions/1
     * {
     *   "requiredPermissionName": "update_park"
     * }
     */
    @PutMapping("/{id}")
    public ResponseEntity<EndpointPermission> updateEndpointPermission(
            @PathVariable Long id,
            @RequestBody EndpointPermissionDTO dto) {

        log.info("Updating endpoint permission: {}", id);

        EndpointPermission updated = endpointPermissionService.updateEndpointPermission(
            id,
            dto.getRequiredPermissionName()
        );

        return ResponseEntity.ok(updated);
    }

    /**
     * Enable/disable endpoint
     *
     * PATCH /api/admin/endpoint-permissions/1/active?active=false
     */
    @PatchMapping("/{id}/active")
    public ResponseEntity<EndpointPermission> setEndpointActive(
            @PathVariable Long id,
            @RequestParam Boolean active) {

        log.info("Setting endpoint {} active: {}", id, active);

        EndpointPermission updated = endpointPermissionService.setEndpointActive(id, active);
        return ResponseEntity.ok(updated);
    }

    /**
     * Delete/disable endpoint permission mapping
     *
     * DELETE /api/admin/endpoint-permissions/1
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEndpoint(@PathVariable Long id) {
        log.info("Disabling endpoint permission: {}", id);

        endpointPermissionService.setEndpointActive(id, false);
        return ResponseEntity.noContent().build();
    }

    /**
     * Clear permission caches
     * Useful after bulk changes to endpoint permissions
     *
     * POST /api/admin/endpoint-permissions/cache/clear
     */
    @PostMapping("/cache/clear")
    public ResponseEntity<String> clearCaches() {
        log.info("Clearing endpoint permission caches");

        endpointPermissionService.clearAllCaches();
        return ResponseEntity.ok("Caches cleared successfully");
    }
}

/**
 * DTO for endpoint permission requests
 */
class EndpointPermissionDTO {

    public String httpMethod;
    public String endpoint;
    public String requiredPermissionName;
    public String actionCode;
    public String resourceType;
    public String allowedRoles;
    public String description;

    public String getHttpMethod() {
        return httpMethod;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public String getRequiredPermissionName() {
        return requiredPermissionName;
    }

    public String getActionCode() {
        return actionCode;
    }

    public String getResourceType() {
        return resourceType;
    }

    public String getAllowedRoles() {
        return allowedRoles;
    }

    public String getDescription() {
        return description;
    }
}
