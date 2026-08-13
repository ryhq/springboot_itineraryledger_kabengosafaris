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
import com.itineraryledger.kabengosafaris.Permission.Specifications.PermissionFilter;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Response.ListStats;
import com.itineraryledger.kabengosafaris.Role.RoleRepository;
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
    private final RoleRepository roleRepository;
    private final ListStats listStats;

    @Autowired
    public PermissionService(
        PermissionRepository permissionRepository,
        IdObfuscator idObfuscator,
        RoleRepository roleRepository,
        ListStats listStats
    ) {
        this.permissionRepository = permissionRepository;
        this.idObfuscator = idObfuscator;
        this.roleRepository = roleRepository;
        this.listStats = listStats;
    }

    private static final List<String> VALID_SORT_FIELDS = java.util.Arrays.asList(
        "name", "entity", "action", "active", "createdAt", "updatedAt");
    private static final String DEFAULT_SORT_FIELD = "name";

    /**
     * The catalogue: the rows, the counters and the sort, in one response.
     *
     * Sorted by name rather than by creation date, because "when was this permission
     * seeded" is nobody's question — the list is read looking for a capability, and the
     * name is what it is looked up by.
     */
    public ResponseEntity<?> getAllPermissions(
        PermissionFilter filter,
        Boolean includeStats,
        Integer page,
        Integer size,
        String sortBy,
        String sortDirection
    ) {
        try {
            PermissionFilter resolved = filter != null ? filter : new PermissionFilter();

            String resolvedSort = sortBy != null && VALID_SORT_FIELDS.contains(sortBy)
                ? sortBy : DEFAULT_SORT_FIELD;
            Sort.Direction direction = "desc".equalsIgnoreCase(sortDirection)
                ? Sort.Direction.DESC : Sort.Direction.ASC;
            // clamp: an unbounded size is a way to ask for all five hundred by accident
            int pageSize = size == null || size <= 0 ? 20 : Math.min(size, 100);
            Pageable paging = PageRequest.of(
                page == null || page < 0 ? 0 : page, pageSize, Sort.by(direction, resolvedSort));

            Specification<Permission> specification = buildSpec(resolved);
            Page<Permission> pagedPermissions = permissionRepository.findAll(specification, paging);

            Map<String, Object> response = new HashMap<>();
            response.put("permissions", withRoleCounts(pagedPermissions.getContent()));
            response.put("currentPage", pagedPermissions.getNumber());
            response.put("totalItems", pagedPermissions.getTotalElements());
            response.put("totalPages", pagedPermissions.getTotalPages());
            response.put("pageSize", pagedPermissions.getSize());
            response.put("validSortFields", VALID_SORT_FIELDS);
            response.put("currentSortBy", resolvedSort);
            response.put("currentSortDirection", direction.name().toLowerCase());
            if (!Boolean.FALSE.equals(includeStats)) {
                response.put("stats", buildStats(specification));
            }

            return ResponseEntity.ok(
                ApiResponse.success(200, "Successfully retrieved permissions.", response));
        } catch (Exception e) {
            log.error("Error listing permissions", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to list permissions", "PERMISSIONS_LIST_FAILED"));
        }
    }

    /** ONE specification, shared by the rows and the counters. */
    private Specification<Permission> buildSpec(PermissionFilter filter) {
        Specification<Permission> spec = Specification.<Permission>unrestricted()
            .and(PermissionSpecification.searchKeyword(filter.effectiveKeyword()))
            .and(PermissionSpecification.nameLike(filter.getName()))
            .and(PermissionSpecification.byEntities(filter.allEntities()))
            .and(PermissionSpecification.byActions(filter.allActions()));

        // contradictory pairs cancel to no constraint, as everywhere else
        boolean wantsActive = filter.hasStatus("active");
        boolean wantsInactive = filter.hasStatus("inactive");
        if (wantsActive != wantsInactive) {
            spec = spec.and(PermissionSpecification.isActive(wantsActive));
        } else if (filter.getActive() != null) {
            spec = spec.and(PermissionSpecification.isActive(filter.getActive()));
        }

        Specification<Permission> quality = null;
        if (filter.wants("noRoles")) quality = PermissionSpecification.hasNoRoles();
        if (filter.wants("custom")) {
            quality = quality == null
                ? PermissionSpecification.isCustom()
                : quality.or(PermissionSpecification.isCustom());
        }
        if (quality != null) spec = spec.and(quality);

        return spec;
    }

    /**
     * The rows, each with how many roles grant it.
     *
     * One grouped query for the page rather than one per row. A zero here is the
     * finding: a capability nothing grants is one nobody in the company has.
     */
    private List<PermissionDTO> withRoleCounts(List<Permission> permissions) {
        List<PermissionDTO> dtos = getPermissionDTOs(permissions);
        if (permissions.isEmpty()) return dtos;

        Map<Long, Long> counts = new HashMap<>();
        try {
            List<Long> ids = permissions.stream().map(Permission::getId).toList();
            for (Object[] row : roleRepository.countRolesByPermissionIds(ids)) {
                counts.put(((Number) row[0]).longValue(), ((Number) row[1]).longValue());
            }
        } catch (Exception e) {
            // A count we cannot get is left null, which the UI renders as "—".
            log.warn("Could not count permission holders", e);
        }

        for (int i = 0; i < permissions.size(); i++) {
            Long count = counts.get(permissions.get(i).getId());
            dtos.get(i).setRoleCount(count == null ? 0 : count.intValue());
        }
        return dtos;
    }

    /**
     * The cards that head the catalogue.
     *
     * The breakdown is by action rather than by entity: there are a hundred and five
     * entities and ten actions, and a hundred and five cards is not a dashboard.
     */
    private Map<String, Object> buildStats(Specification<Permission> spec) {
        return listStats.of(Permission.class, spec)
            .total()
            .count("active", PermissionSpecification.isActive(true))
            .complement("inactive", "active")
            .count("noRoles", PermissionSpecification.hasNoRoles())
            .count("custom", PermissionSpecification.isCustom())
            .breakdown("byAction", PermissionAction.values(), PermissionSpecification::hasAction)
            .build();
    }

    /**
     * Every entity the catalogue covers, sorted, for the filter dropdown.
     *
     * Read from the permissions themselves rather than from entities.json, so the list
     * reflects what is actually grantable — including the custom permissions, whose
     * entities are not all in that file.
     */
    public ResponseEntity<ApiResponse<?>> getDistinctEntities() {
        try {
            List<String> entities = permissionRepository.findAllDistinctEntities()
                .stream().filter(e -> e != null && !e.isBlank()).sorted().toList();
            Map<String, Object> data = new HashMap<>();
            data.put("entities", entities);
            data.put("totalItems", entities.size());
            return ResponseEntity.ok(ApiResponse.success(200, "Entities retrieved", data));
        } catch (Exception e) {
            log.error("Error listing permission entities", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to list entities", "PERMISSION_ENTITIES_FAILED"));
        }
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

            PermissionDTO permissionDTO = convertToDTO(permission);

            // Circular navigation
            Long nextId = permissionRepository.findNextId(id).orElse(null);
            Long previousId = permissionRepository.findPreviousId(id).orElse(null);
            if (nextId == null) nextId = permissionRepository.findFirstId().orElse(null);
            if (previousId == null) previousId = permissionRepository.findLastId().orElse(null);

            Map<String, Object> response = new HashMap<>();
            response.put("permission", permissionDTO);
            response.put("nextId", nextId != null ? idObfuscator.encodeId(nextId) : null);
            response.put("previousId", previousId != null ? idObfuscator.encodeId(previousId) : null);

            return ResponseEntity.ok(
                ApiResponse.success(
                    200,
                    "Successfully retrieved permission.",
                    response
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

            Map<String, Object> response = new HashMap<>();
            response.put("permission", convertToDTO(permission));

            return ResponseEntity.ok(
                ApiResponse.success(
                    200,
                    "Successfully toggled permission active status to " + newStatus,
                    response
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
