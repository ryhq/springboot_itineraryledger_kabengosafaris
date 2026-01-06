package com.itineraryledger.kabengosafaris.Role.Services;

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

import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Role.Role;
import com.itineraryledger.kabengosafaris.Role.RoleRepository;
import com.itineraryledger.kabengosafaris.Role.DTOs.RoleDTO;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.extern.slf4j.Slf4j;

/**
 * RoleGetService - Service for retrieving and filtering roles with pagination
 *
 * This service provides methods to fetch roles with:
 * - Specification-based dynamic filtering
 * - Pagination and sorting support
 * - Obfuscated ID conversion for DTOs
 */
@Service
@Slf4j
public class RoleGetService {

    private final RoleRepository roleRepository;
    private final IdObfuscator idObfuscator;

    @Autowired
    public RoleGetService(RoleRepository roleRepository, IdObfuscator idObfuscator) {
        this.roleRepository = roleRepository;
        this.idObfuscator = idObfuscator;
    }

    /**
     * Get all roles with pagination, filtering, and sorting
     * Controller-style method for API endpoints
     *
     * @param page Page number (0-based)
     * @param size Page size
     * @param name Filter by role name (partial match)
     * @param displayName Filter by display name (partial match)
     * @param active Filter by active status
     * @param isSystemRole Filter by system role status
     * @param sortDir Sort direction ("asc" or "desc")
     * @return ResponseEntity with paginated results or validation error
     */
    public ResponseEntity<?> getAllRoles(
        int page,
        int size,
        String name,
        String displayName,
        Boolean active,
        Boolean isSystemRole,
        String sortDir
    ) {

        log.debug("Fetching roles with filters - page: {}, size: {}, name: {}, displayName: {}, " +
                "active: {}, isSystemRole: {}, sortDir: {}",
                page, size, name, displayName, active, isSystemRole, sortDir);

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
        if ("asc".equalsIgnoreCase(sortDir)) {
            direction = Sort.Direction.ASC;
        }

        Pageable paging = PageRequest.of(
            page,
            size,
            Sort.by(direction, "createdAt")
        );

        // Build dynamic specification
        Specification<Role> specification = Specification.unrestricted();

        if (name != null && !name.isBlank()) {
            specification = specification.and(RoleSpecification.nameLike(name));
        }

        if (displayName != null && !displayName.isBlank()) {
            specification = specification.and(RoleSpecification.displayNameLike(displayName));
        }

        if (active != null) {
            specification = specification.and(RoleSpecification.isActive(active));
        }

        if (isSystemRole != null) {
            specification = specification.and(RoleSpecification.isSystemRole(isSystemRole));
        }

        // Execute query with specifications
        Page<Role> pagedRoles = roleRepository.findAll(specification, paging);

        // Convert to DTOs
        List<RoleDTO> roleDTOs = getRoleDTOs(pagedRoles.getContent());

        // Build response
        Map<String, Object> response = new HashMap<>();
        response.put("roles", roleDTOs);
        response.put("currentPage", pagedRoles.getNumber());
        response.put("totalItems", pagedRoles.getTotalElements());
        response.put("totalPages", pagedRoles.getTotalPages());

        log.info("Successfully fetched {} roles on page {}", roleDTOs.size(), page);
        return ResponseEntity.ok(
            ApiResponse.success(
                200,
                "Successfully retrieved roles.",
                response
            )
        );
    }

    /**
     * Get all roles for a specific user with pagination, filtering, and sorting
     *
     * @param userId The user's ID (non-obfuscated)
     * @param page Page number (0-based)
     * @param size Page size
     * @param name Filter by role name (partial match)
     * @param displayName Filter by display name (partial match)
     * @param active Filter by active status
     * @param isSystemRole Filter by system role status
     * @param sortDir Sort direction ("asc" or "desc")
     * @return ResponseEntity with paginated results or validation error
     */
    public ResponseEntity<?> getRolesForUser(
        Long userId,
        int page,
        int size,
        String name,
        String displayName,
        Boolean active,
        Boolean isSystemRole,
        String sortDir
    ) {

        log.debug("Fetching roles for user {} with filters - page: {}, size: {}, name: {}, displayName: {}, " +
                "active: {}, isSystemRole: {}, sortDir: {}",
                userId, page, size, name, displayName, active, isSystemRole, sortDir);

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
        if ("asc".equalsIgnoreCase(sortDir)) {
            direction = Sort.Direction.ASC;
        }

        Pageable paging = PageRequest.of(
            page,
            size,
            Sort.by(direction, "createdAt")
        );

        // Build dynamic specification - ALWAYS filter by user
        Specification<Role> specification = RoleSpecification.hasUser(userId);

        // Add additional filters
        if (name != null && !name.isBlank()) {
            specification = specification.and(RoleSpecification.nameLike(name));
        }

        if (displayName != null && !displayName.isBlank()) {
            specification = specification.and(RoleSpecification.displayNameLike(displayName));
        }

        if (active != null) {
            specification = specification.and(RoleSpecification.isActive(active));
        }

        if (isSystemRole != null) {
            specification = specification.and(RoleSpecification.isSystemRole(isSystemRole));
        }

        // Execute query with specifications
        Page<Role> pagedRoles = roleRepository.findAll(specification, paging);

        // Convert to DTOs
        List<RoleDTO> roleDTOs = getRoleDTOs(pagedRoles.getContent());

        // Build response
        Map<String, Object> response = new HashMap<>();
        response.put("roles", roleDTOs);
        response.put("currentPage", pagedRoles.getNumber());
        response.put("totalItems", pagedRoles.getTotalElements());
        response.put("totalPages", pagedRoles.getTotalPages());

        log.info("Successfully fetched {} roles for user {} on page {}", roleDTOs.size(), userId, page);
        return ResponseEntity.ok(
            ApiResponse.success(
                200,
                "Successfully retrieved user roles.",
                response
            )
        );
    }

    /**
     * Get all roles for a specific permission with pagination, filtering, and sorting
     *
     * @param permissionIdObfuscated The permission's obfuscated ID
     * @param page Page number (0-based)
     * @param size Page size
     * @param name Filter by role name (partial match)
     * @param displayName Filter by display name (partial match)
     * @param active Filter by active status
     * @param isSystemRole Filter by system role status
     * @param sortDir Sort direction ("asc" or "desc")
     * @return ResponseEntity with paginated results or validation error
     */
    public ResponseEntity<?> getRolesForPermission(
        String permissionIdObfuscated,
        int page,
        int size,
        String name,
        String displayName,
        Boolean active,
        Boolean isSystemRole,
        String sortDir
    ) {

        log.debug("Fetching roles for permission {} with filters - page: {}, size: {}, name: {}, displayName: {}, " +
                "active: {}, isSystemRole: {}, sortDir: {}",
                permissionIdObfuscated, page, size, name, displayName, active, isSystemRole, sortDir);

        // Decode obfuscated permission ID
        Long permissionId;
        try {
            permissionId = idObfuscator.decodeId(permissionIdObfuscated);
        } catch (Exception e) {
            log.warn("Invalid permission ID format: {}", permissionIdObfuscated);
            return ResponseEntity.badRequest().body("Invalid permission ID format");
        }

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
        if ("asc".equalsIgnoreCase(sortDir)) {
            direction = Sort.Direction.ASC;
        }

        Pageable paging = PageRequest.of(
            page,
            size,
            Sort.by(direction, "createdAt")
        );

        // Build dynamic specification - ALWAYS filter by permission
        Specification<Role> specification = RoleSpecification.hasPermission(permissionId);

        // Add additional filters
        if (name != null && !name.isBlank()) {
            specification = specification.and(RoleSpecification.nameLike(name));
        }

        if (displayName != null && !displayName.isBlank()) {
            specification = specification.and(RoleSpecification.displayNameLike(displayName));
        }

        if (active != null) {
            specification = specification.and(RoleSpecification.isActive(active));
        }

        if (isSystemRole != null) {
            specification = specification.and(RoleSpecification.isSystemRole(isSystemRole));
        }

        // Execute query with specifications
        Page<Role> pagedRoles = roleRepository.findAll(specification, paging);

        // Convert to DTOs
        List<RoleDTO> roleDTOs = getRoleDTOs(pagedRoles.getContent());

        // Build response
        Map<String, Object> response = new HashMap<>();
        response.put("roles", roleDTOs);
        response.put("currentPage", pagedRoles.getNumber());
        response.put("totalItems", pagedRoles.getTotalElements());
        response.put("totalPages", pagedRoles.getTotalPages());

        log.info("Successfully fetched {} roles for permission {} on page {}", roleDTOs.size(), permissionIdObfuscated, page);
        return ResponseEntity.ok(
            ApiResponse.success(
                200,
                "Successfully retrieved roles with permission.",
                response
            )
        );
    }

    /**
     * Get a single role by obfuscated ID
     *
     * @param idObfuscated The obfuscated role ID
     * @return ResponseEntity with ApiResponse containing role or error
     */
    public ResponseEntity<ApiResponse<?>> getRole(String idObfuscated) {
        try {
            // Decode obfuscated ID
            Long id = idObfuscator.decodeId(idObfuscated);

            Role role = roleRepository.findById(id).orElse(null);

            if (role == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ApiResponse.error(
                        404,
                        "Role not found",
                        "ROLE_NOT_FOUND"
                    )
                );
            }

            return ResponseEntity.ok(
                ApiResponse.success(
                    200,
                    "Successfully retrieved role.",
                    convertToDTO(role)
                )
            );

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(
                    500,
                    "Failed to get role",
                    "GET_ROLE_FAILED"
                )
            );
        }
    }

    /**
     * Convert list of Role entities to RoleDTOs with obfuscated IDs
     *
     * @param roles The entities to convert
     * @return List of RoleDTO with obfuscated IDs
     */
    private List<RoleDTO> getRoleDTOs(List<Role> roles) {
        return roles.stream().map(this::convertToDTO).toList();
    }

    /**
     * Convert Role entity to RoleDTO with obfuscated ID
     *
     * @param role The entity to convert
     * @return RoleDTO with obfuscated ID
     */
    public RoleDTO convertToDTO(Role role) {
        return RoleDTO.builder()
                .id(idObfuscator.encodeId(role.getId()))
                .name(role.getName())
                .displayName(role.getDisplayName())
                .description(role.getDescription())
                .active(role.getActive())
                .isSystemRole(role.getIsSystemRole())
                .createdAt(role.getCreatedAt())
                .updatedAt(role.getUpdatedAt())
                .build();
    }
}
