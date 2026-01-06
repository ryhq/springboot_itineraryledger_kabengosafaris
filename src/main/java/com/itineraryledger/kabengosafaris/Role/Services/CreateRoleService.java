package com.itineraryledger.kabengosafaris.Role.Services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Role.Role;
import com.itineraryledger.kabengosafaris.Role.RoleRepository;
import com.itineraryledger.kabengosafaris.Role.DTOs.CreateRoleDTO;
import com.itineraryledger.kabengosafaris.Role.DTOs.RoleDTO;

import lombok.extern.slf4j.Slf4j;

/**
 * CreateRoleService - Service for creating and validating new roles
 *
 * This service handles:
 * - Request validation
 * - Duplicate name checks
 * - Entity creation and persistence
 * - Response formatting with ApiResponse
 */
@Service
@Slf4j
public class CreateRoleService {

    private final RoleRepository roleRepository;
    private final RoleGetService roleGetService;

    @Autowired
    public CreateRoleService(
        RoleRepository roleRepository,
        RoleGetService roleGetService
    ) {
        this.roleRepository = roleRepository;
        this.roleGetService = roleGetService;
    }

    /**
     * Create a new role with validation
     *
     * @param createDTO The request DTO containing role details
     * @return ResponseEntity with ApiResponse containing created role or error
     */
    @AuditLogAnnotation(
        action = "CREATE_ROLE", 
        description = "Creating a new role", 
        entityType = "Role"
    )
    public ResponseEntity<ApiResponse<?>> createRole(CreateRoleDTO createDTO) {
        log.info("Creating new role: {}", createDTO.getDisplayName());

        try {
            // Generate name from displayName if not provided
            String roleName = createDTO.getName();
            if (roleName == null || roleName.isBlank()) {
                roleName = createDTO.getDisplayName()
                    .toLowerCase()
                    .replaceAll("\\s+", "_")
                    .replaceAll("[^a-z0-9_]", "");
            } else {
                // Normalize provided name
                roleName = roleName.toLowerCase().trim();
            }

            // Check for duplicate name
            if (roleRepository.existsByName(roleName)) {
                log.warn("Role name already exists: {}", roleName);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        "Role name already exists",
                        "DUPLICATE_ROLE_NAME"
                    )
                );
            }

            // Create role entity
            Role role = Role.builder()
                .name(roleName)
                .displayName(createDTO.getDisplayName())
                .description(createDTO.getDescription())
                .active(createDTO.getActive() != null ? createDTO.getActive() : true)
                .isSystemRole(false)
                .build();

            // Save to database
            Role savedRole = roleRepository.save(role);

            log.info("Role created successfully with ID: {}", savedRole.getId());

            // Create response with obfuscated ID
            RoleDTO roleDTO = roleGetService.convertToDTO(savedRole);

            return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success(
                    201,
                    "Role created successfully",
                    roleDTO
                )
            );

        } catch (Exception e) {
            log.error("Error creating role", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to create role", "ROLE_CREATE_FAILED")
            );
        }
    }
}
