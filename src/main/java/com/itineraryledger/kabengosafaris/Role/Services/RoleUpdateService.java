package com.itineraryledger.kabengosafaris.Role.Services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Role.Role;
import com.itineraryledger.kabengosafaris.Role.RoleRepository;
import com.itineraryledger.kabengosafaris.Role.DTOs.RoleDTO;
import com.itineraryledger.kabengosafaris.Role.DTOs.UpdateRoleDTO;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.extern.slf4j.Slf4j;

/**
 * RoleUpdateService - Service for updating existing roles
 *
 * This service handles:
 * - Partial updates (only update provided fields)
 * - Duplicate name validation
 * - System role protection (cannot modify system roles)
 * - Response formatting with full RoleDTO
 */
@Service
@Slf4j
@Transactional
public class RoleUpdateService {

    /** The one role that must never be switched off; see the guard in updateRole. */
    private static final String SUPERADMIN = "SUPERADMIN";

    private final RoleRepository roleRepository;
    private final RoleGetService roleGetService;
    private final IdObfuscator idObfuscator;

    @Autowired
    public RoleUpdateService(
        RoleRepository roleRepository,
        RoleGetService roleGetService,
        IdObfuscator idObfuscator
    ) {
        this.roleRepository = roleRepository;
        this.roleGetService = roleGetService;
        this.idObfuscator = idObfuscator;
    }

    /**
     * Update an existing role with partial updates
     *
     * @param idObfuscated The obfuscated role ID
     * @param updateDTO The DTO containing fields to update (only provided fields will be updated)
     * @return ResponseEntity with ApiResponse containing updated role or error
     */
    @AuditLogAnnotation(action = "UPDATE_ROLE", description = "Updating a role", entityType = "Role", entityIdParamName = "idObfuscated")
    public ResponseEntity<ApiResponse<?>> updateRole(String idObfuscated, UpdateRoleDTO updateDTO) {
        log.info("Updating role with ID: {}", idObfuscated);

        try {
            // Decode obfuscated ID
            Long id = idObfuscator.decodeId(idObfuscated);

            return updateRole(updateDTO, id);

        } catch (Exception e) {
            log.error("Error updating role", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(
                    500,
                    "Failed to update role",
                    "ROLE_UPDATE_FAILED"
                )
            );
        }
    }

    private ResponseEntity<ApiResponse<?>> updateRole(UpdateRoleDTO updateDTO, Long id) {
        // Find existing role
        Role existing = roleRepository.findById(id).orElse(null);

        if (existing == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                ApiResponse.error(
                    404,
                    "Role not found",
                    "ROLE_NOT_FOUND"
                )
            );
        }

        /*
         * Built-in roles are read-only apart from the one switch that is genuinely useful.
         *
         * SUPERADMIN, ADMIN, USER and GUEST have their definition owned by RoleInitializer,
         * which rewrites it on every startup — so editing a name, a description or a
         * permission here is undone the next time the application restarts, and accepting a
         * write that will quietly vanish is worse than refusing it, because the person who
         * made it believes it held.
         *
         * The active flag is different: it is not part of the definition the initializer
         * rewrites, so switching a built-in role off sticks, and it is the fastest legitimate
         * way to suspend a whole group's access without unpicking assignments.
         */
        if (Boolean.TRUE.equals(existing.getIsSystemRole())) {
            boolean editsDefinition =
                (updateDTO.getName() != null && !updateDTO.getName().isBlank())
                    || (updateDTO.getDisplayName() != null && !updateDTO.getDisplayName().isBlank())
                    || updateDTO.getDescription() != null;

            if (editsDefinition) {
                log.warn("Attempted to edit the definition of system role: {}", existing.getName());
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        "A built-in role's name, description and permissions are read-only — the system "
                            + "rewrites them on every restart, so the change would be undone. It can be "
                            + "switched off, or you can create your own role.",
                        "SYSTEM_ROLE_PROTECTED"
                    )
                );
            }
        }

        /*
         * SUPERADMIN is always active.
         *
         * Switching it off drops every permission from everybody holding it, which includes
         * whoever is doing the switching — and the endpoint that would switch it back on is
         * one of the permissions just lost. There is no route back from inside the
         * application, so this is refused rather than confirmed.
         */
        if (SUPERADMIN.equalsIgnoreCase(existing.getName()) && Boolean.FALSE.equals(updateDTO.getActive())) {
            log.warn("Refused an attempt to deactivate {}", SUPERADMIN);
            return ResponseEntity.badRequest().body(
                ApiResponse.error(
                    400,
                    "Super Administrator is always active. Switching it off would remove every "
                        + "permission from everybody holding it, including the one needed to switch it "
                        + "back on — there would be no way back in.",
                    "SUPERADMIN_ALWAYS_ACTIVE"
                )
            );
        }

        // Update name (if provided and different)
        if (updateDTO.getName() != null && !updateDTO.getName().isBlank()) {
            String normalizedName = updateDTO.getName().toLowerCase().trim();
            if (!existing.getName().equals(normalizedName)) {
                // Check for duplicate name
                if (roleRepository.existsByName(normalizedName)) {
                    log.warn("Role name already exists: {}", normalizedName);
                    return ResponseEntity.badRequest().body(
                        ApiResponse.error(
                            400,
                            "Role name already exists",
                            "DUPLICATE_ROLE_NAME"
                        )
                    );
                }
                existing.setName(normalizedName);
            }
        }

        // Update display name (if provided)
        if (updateDTO.getDisplayName() != null && !updateDTO.getDisplayName().isBlank()) {
            existing.setDisplayName(updateDTO.getDisplayName());
        }

        // Update description (if provided)
        if (updateDTO.getDescription() != null) {
            existing.setDescription(updateDTO.getDescription());
        }

        // Update active status (if provided)
        if (updateDTO.getActive() != null) {
            existing.setActive(updateDTO.getActive());
        }

        // Save updated role
        Role updated = roleRepository.save(existing);

        log.info("Role updated successfully: {}", id);

        // Convert to DTO and return
        RoleDTO roleDTO = roleGetService.convertToDTO(updated);

        return ResponseEntity.ok().body(
            ApiResponse.success(
                200,
                "Role updated successfully",
                roleDTO
            )
        );
    }
}
