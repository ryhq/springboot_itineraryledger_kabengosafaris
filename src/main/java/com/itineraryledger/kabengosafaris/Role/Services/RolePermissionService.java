package com.itineraryledger.kabengosafaris.Role.Services;

import com.itineraryledger.kabengosafaris.Permission.Permission;
import com.itineraryledger.kabengosafaris.Permission.PermissionAction;
import com.itineraryledger.kabengosafaris.Permission.PermissionRepository;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Response.ErrorCode;
import com.itineraryledger.kabengosafaris.Role.DTOs.EntityPermissionsDTO;
import com.itineraryledger.kabengosafaris.Role.DTOs.EntitySummaryDTO;
import com.itineraryledger.kabengosafaris.Role.DTOs.RolePermissionItemDTO;
import com.itineraryledger.kabengosafaris.Role.DTOs.UpdateRolePermissionsDTO;
import com.itineraryledger.kabengosafaris.Role.Role;
import com.itineraryledger.kabengosafaris.Role.RoleRepository;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * RolePermissionService - Service for managing role permissions
 *
 * Provides operations for:
 * - Getting all entities with permission counts for a role (API 2)
 * - Getting detailed permissions for a specific entity (API 3)
 * - Updating role permissions (future)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RolePermissionService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final IdObfuscator idObfuscator;

    /**
     * Get all entities with permission summary for a role
     *
     * @param roleId The obfuscated role ID
     * @return ResponseEntity with list of entities and their permission counts
     */
    public ResponseEntity<ApiResponse<List<EntitySummaryDTO>>> getEntitiesForRole(String roleId) {
        log.info("Getting entities for role: {}", roleId);

        // Decode and validate role ID
        Long decodedRoleId;
        try {
            decodedRoleId = idObfuscator.decodeId(roleId);
        } catch (Exception e) {
            log.error("Invalid role ID: {}", roleId, e);
            return ResponseEntity.badRequest().body(
                ApiResponse.error(
                    HttpStatus.BAD_REQUEST.value(),
                    "Invalid role ID format",
                    ErrorCode.INVALID_INPUT.getCode()
                )
            );
        }

        // Find role
        Optional<Role> roleOpt = roleRepository.findById(decodedRoleId);
        if (roleOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                ApiResponse.error(
                    HttpStatus.NOT_FOUND.value(),
                    "Role not found",
                    ErrorCode.RESOURCE_NOT_FOUND.getCode()
                )
            );
        }

        Role role = roleOpt.get();

        // Get all permissions grouped by entity
        List<Permission> allPermissions = permissionRepository.findAll();
        Map<String, List<Permission>> permissionsByEntity = allPermissions.stream()
            .collect(Collectors.groupingBy(Permission::getEntity));

        // Get role's permission IDs for quick lookup
        Set<Long> rolePermissionIds = role.getPermissions().stream()
            .map(Permission::getId)
            .collect(Collectors.toSet());

        // Build entity summaries
        List<EntitySummaryDTO> entities = new ArrayList<>();
        for (Map.Entry<String, List<Permission>> entry : permissionsByEntity.entrySet()) {
            String entityName = entry.getKey();
            List<Permission> entityPermissions = entry.getValue();

            int totalPermissions = entityPermissions.size();
            int assignedPermissions = (int) entityPermissions.stream()
                .filter(p -> rolePermissionIds.contains(p.getId()))
                .count();
            int unassignedPermissions = totalPermissions - assignedPermissions;

            double percentage = totalPermissions > 0
                ? (assignedPermissions * 100.0 / totalPermissions)
                : 0.0;

            EntitySummaryDTO entitySummary = EntitySummaryDTO.builder()
                .entity(entityName)
                .entityDisplayName(formatEntityDisplayName(entityName))
                .totalPermissions(totalPermissions)
                .assignedPermissions(assignedPermissions)
                .unassignedPermissions(unassignedPermissions)
                .assignmentPercentage(Math.round(percentage * 100.0) / 100.0)
                .build();

            entities.add(entitySummary);
        }

        // Sort alphabetically by entity name
        entities.sort(Comparator.comparing(EntitySummaryDTO::getEntity));

        log.info("Found {} entities for role {}", entities.size(), role.getName());

        return ResponseEntity.ok(
            ApiResponse.success(
                HttpStatus.OK.value(),
                "Entities retrieved successfully",
                entities
            )
        );
    }

    /**
     * Get detailed permissions for a specific entity with assignment status
     *
     * @param roleId The obfuscated role ID
     * @param entity The entity name (e.g., "USER", "ROLE")
     * @return ResponseEntity with detailed permissions for the entity
     */
    public ResponseEntity<ApiResponse<EntityPermissionsDTO>> getEntityPermissions(
        String roleId,
        String entity
    ) {
        log.info("Getting permissions for role {} and entity {}", roleId, entity);

        // Decode and validate role ID
        Long decodedRoleId;
        try {
            decodedRoleId = idObfuscator.decodeId(roleId);
        } catch (Exception e) {
            log.error("Invalid role ID: {}", roleId, e);
            return ResponseEntity.badRequest().body(
                ApiResponse.error(
                    HttpStatus.BAD_REQUEST.value(),
                    "Invalid role ID format",
                    ErrorCode.INVALID_INPUT.getCode()
                )
            );
        }

        // Find role
        Optional<Role> roleOpt = roleRepository.findById(decodedRoleId);
        if (roleOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                ApiResponse.error(
                    HttpStatus.NOT_FOUND.value(),
                    "Role not found",
                    ErrorCode.RESOURCE_NOT_FOUND.getCode()
                )
            );
        }

        Role role = roleOpt.get();

        // Get all permissions for this entity
        List<Permission> entityPermissions = permissionRepository.findByEntity(entity.toUpperCase());
        if (entityPermissions.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                ApiResponse.error(
                    HttpStatus.NOT_FOUND.value(),
                    "No permissions found for entity: " + entity,
                    ErrorCode.RESOURCE_NOT_FOUND.getCode()
                )
            );
        }

        // Get role's permission IDs for quick lookup
        Set<Long> rolePermissionIds = role.getPermissions().stream()
            .map(Permission::getId)
            .collect(Collectors.toSet());

        // Build permission items with assignment status
        List<RolePermissionItemDTO> permissionItems = entityPermissions.stream()
            .map(permission -> {
                boolean isAssigned = rolePermissionIds.contains(permission.getId());

                return RolePermissionItemDTO.builder()
                    .id(idObfuscator.encodeId(permission.getId()))
                    .name(permission.getName())
                    .description(permission.getDescription())
                    .action(permission.getAction())
                    .actionDisplayName(formatActionDisplayName(permission.getAction().name()))
                    .entity(permission.getEntity())
                    .assigned(isAssigned)
                    .active(permission.getActive())
                    .createdAt(permission.getCreatedAt())
                    .updatedAt(permission.getUpdatedAt())
                    .build();
            })
            .sorted(Comparator.comparing(p -> p.getAction().ordinal()))
            .collect(Collectors.toList());

        // Calculate counts
        int totalPermissions = permissionItems.size();
        int assignedPermissions = (int) permissionItems.stream()
            .filter(RolePermissionItemDTO::getAssigned)
            .count();
        int unassignedPermissions = totalPermissions - assignedPermissions;

        // Build response
        EntityPermissionsDTO response = EntityPermissionsDTO.builder()
            .entity(entity.toUpperCase())
            .entityDisplayName(formatEntityDisplayName(entity))
            .permissions(permissionItems)
            .totalPermissions(totalPermissions)
            .assignedPermissions(assignedPermissions)
            .unassignedPermissions(unassignedPermissions)
            .build();

        log.info("Found {} permissions for entity {} ({} assigned, {} unassigned)",
            totalPermissions, entity, assignedPermissions, unassignedPermissions);

        return ResponseEntity.ok(
            ApiResponse.success(
                HttpStatus.OK.value(),
                "Entity permissions retrieved successfully",
                response
            )
        );
    }
    
    /**
     * Update role permissions for a specific entity
     *
     * This method replaces all permissions for the given entity:
     * - Permissions in the request will be ASSIGNED to the role
     * - Permissions NOT in the request will be REMOVED from the role
     *
     * @param roleId The obfuscated role ID
     * @param entity The entity name (e.g., "USER", "ROLE")
     * @param updateDTO The DTO containing permission IDs to assign
     * @return ResponseEntity with updated entity permissions
     */
    @Transactional
    public ResponseEntity<ApiResponse<EntityPermissionsDTO>> updateRolePermissions(
        String roleId,
        String entity,
        UpdateRolePermissionsDTO updateDTO
    ) {
        log.info("Updating permissions for role {} and entity {}", roleId, entity);

        // Decode and validate role ID
        Long decodedRoleId;
        try {
            decodedRoleId = idObfuscator.decodeId(roleId);
        } catch (Exception e) {
            log.error("Invalid role ID: {}", roleId, e);
            return ResponseEntity.badRequest().body(
                ApiResponse.error(
                    HttpStatus.BAD_REQUEST.value(),
                    "Invalid role ID format",
                    ErrorCode.INVALID_INPUT.getCode()
                )
            );
        }

        // Find role
        Optional<Role> roleOpt = roleRepository.findById(decodedRoleId);
        if (roleOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                ApiResponse.error(
                    HttpStatus.NOT_FOUND.value(),
                    "Role not found",
                    ErrorCode.RESOURCE_NOT_FOUND.getCode()
                )
            );
        }

        Role role = roleOpt.get();

        // Get all permissions for this entity
        List<Permission> entityPermissions = permissionRepository.findByEntity(entity.toUpperCase());
        if (entityPermissions.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                ApiResponse.error(
                    HttpStatus.NOT_FOUND.value(),
                    "No permissions found for entity: " + entity,
                    ErrorCode.RESOURCE_NOT_FOUND.getCode()
                )
            );
        }

        // Decode permission IDs from request
        Set<Long> requestedPermissionIds = new HashSet<>();
        if (updateDTO.getPermissionIds() != null) {
            for (String permissionId : updateDTO.getPermissionIds()) {
                try {
                    Long decodedPermissionId = idObfuscator.decodeId(permissionId);
                    requestedPermissionIds.add(decodedPermissionId);
                } catch (Exception e) {
                    log.error("Invalid permission ID: {}", permissionId, e);
                    return ResponseEntity.badRequest().body(
                        ApiResponse.error(
                            HttpStatus.BAD_REQUEST.value(),
                            "Invalid permission ID format",
                            ErrorCode.INVALID_INPUT.getCode()
                        )
                    );
                }
            }
        }

        // Create a map of entity permission IDs for validation
        Set<Long> entityPermissionIds = entityPermissions.stream()
            .map(Permission::getId)
            .collect(Collectors.toSet());

        // Validate that all requested permission IDs belong to this entity
        for (Long permissionId : requestedPermissionIds) {
            if (!entityPermissionIds.contains(permissionId)) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        HttpStatus.BAD_REQUEST.value(),
                        "One or more permission IDs do not belong to entity: " + entity,
                        ErrorCode.INVALID_INPUT.getCode()
                    )
                );
            }
        }

        // Get current role permissions
        Set<Permission> currentPermissions = role.getPermissions();

        // Remove all existing permissions for this entity from the role
        List<Permission> permissionsToRemove = currentPermissions.stream()
            .filter(p -> p.getEntity().equalsIgnoreCase(entity))
            .collect(Collectors.toList());

        for (Permission permission : permissionsToRemove) {
            role.removePermission(permission);
            log.debug("Removed permission {} from role {}", permission.getName(), role.getName());
        }

        // Add new permissions for this entity to the role
        int addedCount = 0;
        for (Permission permission : entityPermissions) {
            if (requestedPermissionIds.contains(permission.getId())) {
                role.addPermission(permission);
                addedCount++;
                log.debug("Added permission {} to role {}", permission.getName(), role.getName());
            }
        }

        // Save the updated role
        role = roleRepository.save(role);

        log.info("Updated permissions for role {} on entity {}: removed {}, added {}",
            role.getName(), entity, permissionsToRemove.size(), addedCount);

        // Return the updated entity permissions (same as GET endpoint)
        return getEntityPermissions(roleId, entity);
    }

    /**
     * Reset permissions for a specific role and entity to system defaults
     *
     * This will set permissions for the given entity on the specified role according to the default configuration:
     * - SUPERADMIN: CREATE, READ, UPDATE, DELETE
     * - ADMIN: CREATE, READ, UPDATE (no DELETE)
     * - USER: CREATE, READ (no UPDATE, no DELETE)
     * - GUEST: READ only
     *
     * @param roleId The obfuscated role ID
     * @param entity The entity name (e.g., "USER", "ROLE")
     * @return ResponseEntity with entity permissions (same as getEntityPermissions)
     */
    @Transactional
    public ResponseEntity<ApiResponse<EntityPermissionsDTO>> resetRoleEntityPermissionsToDefaults(String roleId, String entity) {
        log.info("Resetting permissions to defaults for roleId: {} and entity: {}", roleId, entity);

        // Decode and validate role ID
        Long decodedRoleId;
        try {
            decodedRoleId = idObfuscator.decodeId(roleId);
        } catch (Exception e) {
            log.error("Invalid role ID format: {}", roleId);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                ApiResponse.error(
                    HttpStatus.BAD_REQUEST.value(),
                    "Invalid role ID format",
                    ErrorCode.INVALID_INPUT.getCode()
                )
            );
        }

        // Find the role
        Optional<Role> roleOptional = roleRepository.findById(decodedRoleId);
        if (roleOptional.isEmpty()) {
            log.error("Role not found for ID: {}", roleId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                ApiResponse.error(
                    HttpStatus.NOT_FOUND.value(),
                    "Role not found",
                    ErrorCode.ROLE_NOT_FOUND.getCode()
                )
            );
        }

        Role role = roleOptional.get();

        // Verify this is a system role
        if (!role.getIsSystemRole()) {
            log.error("Attempt to reset permissions for non-system role: {}", role.getName());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                ApiResponse.error(
                    HttpStatus.BAD_REQUEST.value(),
                    "Permission reset is only allowed for system roles",
                    ErrorCode.OPERATION_NOT_ALLOWED.getCode()
                )
            );
        }

        // Get all permissions for this entity
        List<Permission> entityPermissions = permissionRepository.findByEntity(entity.toUpperCase());
        if (entityPermissions.isEmpty()) {
            log.error("No permissions found for entity: {}", entity);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                ApiResponse.error(
                    HttpStatus.NOT_FOUND.value(),
                    "No permissions found for entity: " + entity,
                    ErrorCode.RESOURCE_NOT_FOUND.getCode()
                )
            );
        }

        // Determine target permissions based on role type
        Set<Permission> targetPermissions = new HashSet<>();

        switch (role.getName()) {
            case "SUPERADMIN":
                // All permissions (CREATE, READ, UPDATE, DELETE)
                targetPermissions.addAll(entityPermissions);
                break;
            case "ADMIN":
                // CREATE, READ, UPDATE (no DELETE)
                for (Permission permission : entityPermissions) {
                    if (permission.getAction() != PermissionAction.DELETE) {
                        targetPermissions.add(permission);
                    }
                }
                break;
            case "USER":
                // CREATE, READ (no UPDATE, no DELETE)
                for (Permission permission : entityPermissions) {
                    if (permission.getAction() == PermissionAction.CREATE ||
                        permission.getAction() == PermissionAction.READ) {
                        targetPermissions.add(permission);
                    }
                }
                break;
            case "GUEST":
                // READ only
                for (Permission permission : entityPermissions) {
                    if (permission.getAction() == PermissionAction.READ) {
                        targetPermissions.add(permission);
                    }
                }
                break;
            default:
                // Unknown system role
                log.error("Unknown system role type: {}", role.getName());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.error(
                        HttpStatus.INTERNAL_SERVER_ERROR.value(),
                        "Unknown system role type",
                        ErrorCode.INTERNAL_SERVER_ERROR.getCode()
                    )
                );
        }

        // Remove all current permissions for this entity from the role
        Set<Permission> currentPermissions = new HashSet<>(role.getPermissions());
        int removedCount = 0;
        for (Permission permission : currentPermissions) {
            if (permission.getEntity().equalsIgnoreCase(entity)) {
                role.removePermission(permission);
                removedCount++;
            }
        }

        // Add the target permissions to the role
        int addedCount = 0;
        for (Permission permission : targetPermissions) {
            role.addPermission(permission);
            addedCount++;
        }

        // Save the role
        roleRepository.save(role);

        log.info("Reset {} role for entity {}: removed {}, added {}, final count: {}",
            role.getName(), entity, removedCount, addedCount, targetPermissions.size());

        // Return the same response as getEntityPermissions
        return getEntityPermissions(roleId, entity);
    }

    /**
     * Format entity name for display
     * Example: "USER" -> "User Management"
     */
    private String formatEntityDisplayName(String entity) {
        if (entity == null) return "";

        String[] words = entity.split("_");
        StringBuilder result = new StringBuilder();

        for (String word : words) {
            if (result.length() > 0) result.append(" ");
            result.append(word.charAt(0))
                  .append(word.substring(1).toLowerCase());
        }

        return result.toString();
    }

    /**
     * Format action name for display
     * Example: "CREATE" -> "Create"
     */
    private String formatActionDisplayName(String action) {
        if (action == null) return "";
        return action.charAt(0) + action.substring(1).toLowerCase();
    }
}
