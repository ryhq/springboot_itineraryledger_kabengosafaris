package com.itineraryledger.kabengosafaris.Permission;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.Permission.DTOs.PermissionDTO;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.extern.slf4j.Slf4j;

/**
 * PermissionService - Service for managing permissions
 *
 * This service provides methods to:
 * - Get all permissions with pagination, filtering, and sorting
 * - Get a single permission by ID
 * - Toggle permission active status
 */
@Service
@Slf4j
public class PermissionService {

    private final PermissionRepository permissionRepository;
    private final IdObfuscator idObfuscator;

    @Autowired
    public PermissionService(PermissionRepository permissionRepository, IdObfuscator idObfuscator) {
        this.permissionRepository = permissionRepository;
        this.idObfuscator = idObfuscator;
    }

    /**
     * Get all permissions with pagination, filtering, and sorting
     *
     * @param page Page number (0-based)
     * @param size Page size
     * @param name Filter by permission name (partial match)
     * @param entity Filter by entity (partial match)
     * @param action Filter by permission action
     * @param active Filter by active status
     * @param sortDirection Sort direction ("asc" or "desc")
     * @return ResponseEntity with paginated results or validation error
     */
    public ResponseEntity<?> getAllPermissions(
        int page,
        int size,
        String name,
        String entity,
        PermissionAction action,
        Boolean active,
        String sortDirection
    ) {

        log.debug("Fetching permissions with filters - page: {}, size: {}, name: {}, entity: {}, " +
                "action: {}, active: {}, sortDirection: {}",
                page, size, name, entity, action, active, sortDirection);

        // Validate pagination parameters
        if (page < 0) {
            log.warn("Invalid page number: {}", page);
            return ResponseEntity.badRequest().body("Page number cannot be negative");
        }
        if (size <= 0) {
            log.warn("Invalid page size: {}", size);
            return ResponseEntity.badRequest().body("Page size must be greater than 0");
        }

        // Setup sorting
        Sort.Direction direction = Sort.Direction.DESC;
        if ("asc".equalsIgnoreCase(sortDirection)) {
            direction = Sort.Direction.ASC;
        }

        Pageable paging = PageRequest.of(
            page,
            size,
            Sort.by(direction, "createdAt")
        );

        // Build dynamic specification
        Specification<Permission> specification = Specification.unrestricted();

        if (name != null && !name.isBlank()) {
            specification = specification.and(PermissionSpecification.nameLike(name));
        }

        if (entity != null && !entity.isBlank()) {
            specification = specification.and(PermissionSpecification.entityLike(entity));
        }

        if (action != null) {
            specification = specification.and(PermissionSpecification.hasAction(action));
        }

        if (active != null) {
            specification = specification.and(PermissionSpecification.isActive(active));
        }

        // Execute query with specifications
        Page<Permission> pagedPermissions = permissionRepository.findAll(specification, paging);

        // Convert to DTOs
        List<PermissionDTO> permissionDTOs = getPermissionDTOs(pagedPermissions.getContent());

        // Build response
        Map<String, Object> response = new HashMap<>();
        response.put("permissions", permissionDTOs);
        response.put("currentPage", pagedPermissions.getNumber());
        response.put("totalItems", pagedPermissions.getTotalElements());
        response.put("totalPages", pagedPermissions.getTotalPages());

        log.info("Successfully fetched {} permissions on page {}", permissionDTOs.size(), page);
        return ResponseEntity.ok(
            ApiResponse.success(
                200,
                "Successfully retrieved permissions.",
                response
            )
        );
    }

    /**
     * Get a single permission by obfuscated ID
     *
     * @param idObfuscated The obfuscated permission ID
     * @return ResponseEntity with ApiResponse containing permission or error
     */
    public ResponseEntity<ApiResponse<?>> getPermission(String idObfuscated) {
        try {
            // Decode obfuscated ID
            Long id = idObfuscator.decodeId(idObfuscated);

            Permission permission = permissionRepository.findById(id).orElse(null);

            if (permission == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ApiResponse.error(
                        404,
                        "Permission not found",
                        "RESOURCE_NOT_FOUND"
                    )
                );
            }

            return ResponseEntity.ok(
                ApiResponse.success(
                    200,
                    "Successfully retrieved permission.",
                    convertToDTO(permission)
                )
            );

        } catch (Exception e) {
            log.error("Error getting permission", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(
                    500,
                    "Failed to get permission",
                    "GET_PERMISSION_FAILED"
                )
            );
        }
    }

    /**
     * Toggle permission active status
     *
     * @param idObfuscated The obfuscated permission ID
     * @return ResponseEntity with ApiResponse containing updated permission or error
     */
    @Transactional
    public ResponseEntity<ApiResponse<?>> togglePermissionActiveStatus(String idObfuscated) {
        try {
            // Decode obfuscated ID
            Long id = idObfuscator.decodeId(idObfuscated);

            Permission permission = permissionRepository.findById(id).orElse(null);

            if (permission == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ApiResponse.error(
                        404,
                        "Permission not found",
                        "RESOURCE_NOT_FOUND"
                    )
                );
            }

            // Toggle active status
            boolean newStatus = !permission.getActive();
            permission.setActive(newStatus);
            permissionRepository.save(permission);

            log.info("Toggled permission {} active status to {}", permission.getName(), newStatus);

            return ResponseEntity.ok(
                ApiResponse.success(
                    200,
                    "Successfully toggled permission active status to " + newStatus,
                    convertToDTO(permission)
                )
            );

        } catch (Exception e) {
            log.error("Error toggling permission active status", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(
                    500,
                    "Failed to toggle permission active status",
                    "TOGGLE_PERMISSION_FAILED"
                )
            );
        }
    }

    /**
     * Convert list of Permission entities to PermissionDTOs with obfuscated IDs
     *
     * @param permissions The entities to convert
     * @return List of PermissionDTO with obfuscated IDs
     */
    private List<PermissionDTO> getPermissionDTOs(List<Permission> permissions) {
        return permissions.stream().map(this::convertToDTO).toList();
    }

    /**
     * Convert Permission entity to PermissionDTO with obfuscated ID
     *
     * @param permission The entity to convert
     * @return PermissionDTO with obfuscated ID
     */
    private PermissionDTO convertToDTO(Permission permission) {
        return PermissionDTO.builder()
                .id(idObfuscator.encodeId(permission.getId()))
                .name(permission.getName())
                .description(permission.getDescription())
                .action(permission.getAction())
                .actionDisplayName(permission.getAction().getDisplayName())
                .entity(permission.getEntity())
                .active(permission.getActive())
                .createdAt(permission.getCreatedAt())
                .updatedAt(permission.getUpdatedAt())
                .build();
    }
}
