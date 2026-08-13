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
         * A built-in role's KEY is protected, and only its key.
         *
         * RoleInitializer looks these roles up by name on every startup. Rename
         * SUPERADMIN and the next restart finds no SUPERADMIN, creates a second one, and
         * the original drifts on as an orphan holding whatever it held — so the name is
         * genuinely not ours to change.
         *
         * Everything else is fair game. Rewording ADMIN's description is harmless, and
         * switching a built-in role off is a legitimate (if drastic) act that was already
         * possible through PATCH /bulk — refusing it here only made the inline switch
         * disagree with the bulk one, which is worse than either answer.
         */
        if (Boolean.TRUE.equals(existing.getIsSystemRole())
            && updateDTO.getName() != null
            && !updateDTO.getName().isBlank()
            && !existing.getName().equalsIgnoreCase(updateDTO.getName().trim())) {
            log.warn("Attempted to rename system role: {}", existing.getName());
            return ResponseEntity.badRequest().body(
                ApiResponse.error(
                    400,
                    "A built-in role's key cannot be changed — the system looks it up by name on every "
                        + "startup. Its name on screen and its description can be edited.",
                    "SYSTEM_ROLE_KEY_PROTECTED"
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
