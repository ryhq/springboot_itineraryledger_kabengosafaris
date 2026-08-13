package com.itineraryledger.kabengosafaris.Role.Services;

import java.util.Arrays;
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

    private static final List<String> VALID_SORT_FIELDS = Arrays.asList(
        "name", "displayName", "active", "isSystemRole", "createdAt", "updatedAt"
    );
    private static final String DEFAULT_SORT_FIELD = "createdAt";

    @Autowired
    public RoleGetService(RoleRepository roleRepository, IdObfuscator idObfuscator) {
        this.roleRepository = roleRepository;
        this.idObfuscator = idObfuscator;
    }

    /*
     * The list itself now lives in RoleListService, on the house contract: counters
     * from the same specification as the rows, and record paging that walks the
     * filtered set. The method that used to be here was replaced rather than kept
     * alongside it — two list paths over one table drift, and then the counters and
     * the rows disagree.
     *
     * What stays here are the two narrow questions the older screens ask: which roles
     * does this user hold, and which roles hold this permission.
     */

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
     * @param sortDirection Sort direction ("asc" or "desc")
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
        String sortBy,
        String sortDirection
    ) {

        log.debug("Fetching roles for user {} with filters - page: {}, size: {}, name: {}, displayName: {}, " +
                "active: {}, isSystemRole: {}, sortBy: {}, sortDirection: {}",
                userId, page, size, name, displayName, active, isSystemRole, sortBy, sortDirection);

        // Validate pagination parameters
        if (page < 0) {
            log.warn("Invalid page number: {}", page);
            return ResponseEntity.badRequest().body("Page number cannot be negative");
        }
        if (size <= 0) {
            log.warn("Invalid page size: {}", size);
            return ResponseEntity.badRequest().body("Page size must be greater than 0");
        }

        // Sorting with validation
        String validatedSortBy = validateSortField(sortBy);
        if (validatedSortBy == null) {
            log.warn("Invalid sort field: {}", sortBy);
            return ResponseEntity.badRequest().body(
                ApiResponse.error(400, "Invalid sort field: " + sortBy + ". Valid fields are: " + VALID_SORT_FIELDS, "INVALID_SORT_FIELD")
            );
        }

        Sort.Direction direction = Sort.Direction.DESC;
        if ("asc".equalsIgnoreCase(sortDirection)) {
            direction = Sort.Direction.ASC;
        }

        Pageable paging = PageRequest.of(
            page,
            size,
            Sort.by(direction, validatedSortBy)
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
        response.put("validSortFields", VALID_SORT_FIELDS);
        response.put("currentSortBy", validatedSortBy);
        response.put("currentSortDirection", sortDirection != null ? sortDirection : "desc");

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
     * @param sortDirection Sort direction ("asc" or "desc")
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
        String sortBy,
        String sortDirection
    ) {

        log.debug("Fetching roles for permission {} with filters - page: {}, size: {}, name: {}, displayName: {}, " +
                "active: {}, isSystemRole: {}, sortBy: {}, sortDirection: {}",
                permissionIdObfuscated, page, size, name, displayName, active, isSystemRole, sortBy, sortDirection);

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

        // Sorting with validation
        String validatedSortBy = validateSortField(sortBy);
        if (validatedSortBy == null) {
            log.warn("Invalid sort field: {}", sortBy);
            return ResponseEntity.badRequest().body(
                ApiResponse.error(400, "Invalid sort field: " + sortBy + ". Valid fields are: " + VALID_SORT_FIELDS, "INVALID_SORT_FIELD")
            );
        }

        Sort.Direction direction = Sort.Direction.DESC;
        if ("asc".equalsIgnoreCase(sortDirection)) {
            direction = Sort.Direction.ASC;
        }

        Pageable paging = PageRequest.of(
            page,
            size,
            Sort.by(direction, validatedSortBy)
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
        response.put("validSortFields", VALID_SORT_FIELDS);
        response.put("currentSortBy", validatedSortBy);
        response.put("currentSortDirection", sortDirection != null ? sortDirection : "desc");

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

            RoleDTO roleDTO = convertToDTO(role);

            // Circular navigation
            Long nextId = roleRepository.findNextId(id).orElse(null);
            Long previousId = roleRepository.findPreviousId(id).orElse(null);
            if (nextId == null) nextId = roleRepository.findFirstId().orElse(null);
            if (previousId == null) previousId = roleRepository.findLastId().orElse(null);

            Map<String, Object> response = new HashMap<>();
            response.put("role", roleDTO);
            response.put("nextId", nextId != null ? idObfuscator.encodeId(nextId) : null);
            response.put("previousId", previousId != null ? idObfuscator.encodeId(previousId) : null);

            return ResponseEntity.ok(
                ApiResponse.success(
                    200,
                    "Successfully retrieved role.",
                    response
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

    private String validateSortField(String sortBy) {
        if (sortBy == null || sortBy.isBlank()) return DEFAULT_SORT_FIELD;
        for (String field : VALID_SORT_FIELDS) {
            if (field.equalsIgnoreCase(sortBy)) return field;
        }
        return null;
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
