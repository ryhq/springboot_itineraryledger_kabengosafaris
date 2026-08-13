package com.itineraryledger.kabengosafaris.Role.Services;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Response.ListStats;
import com.itineraryledger.kabengosafaris.Response.RecordNavigation;
import com.itineraryledger.kabengosafaris.Role.Role;
import com.itineraryledger.kabengosafaris.Role.RoleRepository;
import com.itineraryledger.kabengosafaris.Role.DTOs.RoleDTO;
import com.itineraryledger.kabengosafaris.Role.Specifications.RoleFilter;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import com.itineraryledger.kabengosafaris.User.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * The role list, on the house contract.
 *
 * RoleGetService already answers the two narrow questions the older screens ask —
 * which roles does this user hold, which hold this permission — and those keep their
 * own methods. This is the list itself: one endpoint carrying the rows, the counters
 * computed from the same specification, and record paging that walks the filtered set
 * rather than every role in id order.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class RoleListService {

    private static final List<String> VALID_SORT_FIELDS = Arrays.asList(
        "name", "displayName", "active", "isSystemRole", "createdAt", "updatedAt");
    private static final String DEFAULT_SORT_FIELD = "displayName";

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final RoleGetService roleGetService;
    private final IdObfuscator idObfuscator;
    private final ListStats listStats;
    private final RecordNavigation recordNavigation;

    public ResponseEntity<ApiResponse<?>> list(
        RoleFilter filter,
        Boolean includeStats,
        Integer page,
        Integer size,
        String sortBy,
        String sortDirection
    ) {
        try {
            String resolvedSort = sortBy != null && VALID_SORT_FIELDS.contains(sortBy)
                ? sortBy : DEFAULT_SORT_FIELD;
            Sort.Direction direction = "desc".equalsIgnoreCase(sortDirection)
                ? Sort.Direction.DESC : Sort.Direction.ASC;
            int pageSize = size == null || size <= 0 ? 20 : Math.min(size, 100);
            Pageable pageable = PageRequest.of(
                page == null || page < 0 ? 0 : page, pageSize, Sort.by(direction, resolvedSort));

            Specification<Role> spec = buildSpec(filter != null ? filter : new RoleFilter());
            Page<Role> found = roleRepository.findAll(spec, pageable);

            Map<String, Object> response = new HashMap<>();
            response.put("roles", withCounts(found.getContent()));
            response.put("currentPage", found.getNumber());
            response.put("totalItems", found.getTotalElements());
            response.put("totalPages", found.getTotalPages());
            response.put("pageSize", found.getSize());
            response.put("validSortFields", VALID_SORT_FIELDS);
            response.put("currentSortBy", resolvedSort);
            response.put("currentSortDirection", direction.name().toLowerCase());
            if (!Boolean.FALSE.equals(includeStats)) {
                response.put("stats", buildStats(spec));
            }

            return ResponseEntity.ok(ApiResponse.success(200, "Roles retrieved", response));
        } catch (Exception e) {
            log.error("Error listing roles", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to list roles", "ROLES_LIST_FAILED"));
        }
    }

    /** One role, and where it sits in the set the caller came from. */
    public ResponseEntity<ApiResponse<?>> getOne(
        String idObfuscated,
        RoleFilter filter,
        String sortBy,
        String sortDirection
    ) {
        try {
            Long id = idObfuscator.decodeId(idObfuscated);
            Role role = roleRepository.findById(id).orElse(null);
            if (role == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Role not found", "ROLE_NOT_FOUND"));
            }

            Specification<Role> navSpec = buildSpec(filter != null ? filter : new RoleFilter());
            String navSortBy = sortBy != null && VALID_SORT_FIELDS.contains(sortBy)
                ? sortBy : DEFAULT_SORT_FIELD;
            Map<String, Object> nav = recordNavigation.navigate(
                Role.class, navSpec, navSortBy, !"desc".equalsIgnoreCase(sortDirection), id);

            Long nextId = (Long) nav.get("nextRawId");
            Long previousId = (Long) nav.get("previousRawId");

            Map<String, Object> response = new HashMap<>();
            response.put("role", withCounts(List.of(role)).get(0));
            response.put("nextId", nextId != null ? idObfuscator.encodeId(nextId) : null);
            response.put("previousId", previousId != null ? idObfuscator.encodeId(previousId) : null);
            response.put("position", nav.get("position"));
            response.put("total", nav.get("total"));

            return ResponseEntity.ok(ApiResponse.success(200, "Role retrieved", response));
        } catch (Exception e) {
            log.error("Error fetching role", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to fetch role", "ROLE_FETCH_FAILED"));
        }
    }

    /** ONE specification, shared by the rows, the counters and the record walk. */
    private Specification<Role> buildSpec(RoleFilter filter) {
        Specification<Role> spec = Specification.<Role>unrestricted()
            .and(RoleSpecification.searchKeyword(filter.effectiveKeyword()))
            .and(RoleSpecification.nameLike(filter.getName()))
            .and(RoleSpecification.displayNameLike(filter.getDisplayName()))
            .and(RoleSpecification.createdAfter(atStartOfDay(filter.getCreatedAfter())))
            .and(RoleSpecification.createdBefore(atEndOfDay(filter.getCreatedBefore())));

        spec = narrowById(spec, filter.getPermissionId(), RoleSpecification::hasPermission, "permission");
        spec = narrowById(spec, filter.getUserId(), RoleSpecification::hasUser, "user");

        // contradictory pairs cancel to no constraint, as everywhere else
        boolean wantsActive = filter.hasStatus("active");
        boolean wantsInactive = filter.hasStatus("inactive");
        if (wantsActive != wantsInactive) {
            spec = spec.and(RoleSpecification.isActive(wantsActive));
        } else if (filter.getActive() != null) {
            spec = spec.and(RoleSpecification.isActive(filter.getActive()));
        }

        boolean wantsSystem = filter.hasKind("system");
        boolean wantsCustom = filter.hasKind("custom");
        if (wantsSystem != wantsCustom) {
            spec = spec.and(RoleSpecification.isSystemRole(wantsSystem));
        } else if (filter.getIsSystemRole() != null) {
            spec = spec.and(RoleSpecification.isSystemRole(filter.getIsSystemRole()));
        }

        Specification<Role> quality = null;
        if (filter.wants("noPermissions")) quality = or(quality, RoleSpecification.hasNoPermissions());
        if (filter.wants("noUsers")) quality = or(quality, RoleSpecification.hasNoUsers());
        if (quality != null) spec = spec.and(quality);

        return spec;
    }

    /**
     * Narrows by an obfuscated id, or narrows to nothing if it will not decode.
     *
     * An unreadable id must not quietly widen the list to every role — that is the opposite
     * of what was asked for, and on this screen the list is who can do something.
     */
    private Specification<Role> narrowById(
        Specification<Role> spec,
        String obfuscated,
        java.util.function.Function<Long, Specification<Role>> by,
        String what
    ) {
        if (obfuscated == null || obfuscated.isBlank()) return spec;
        try {
            return spec.and(by.apply(idObfuscator.decodeId(obfuscated)));
        } catch (Exception e) {
            log.warn("Unreadable {} id on the roles filter: {}", what, obfuscated);
            return spec.and((root, query, cb) -> cb.disjunction());
        }
    }

    private Specification<Role> or(Specification<Role> spec, Specification<Role> extra) {
        return spec == null ? extra : spec.or(extra);
    }

    private LocalDateTime atStartOfDay(LocalDate date) {
        return date == null ? null : date.atStartOfDay();
    }

    private LocalDateTime atEndOfDay(LocalDate date) {
        return date == null ? null : date.atTime(23, 59, 59);
    }

    /**
     * The rows, with their permission and holder counts.
     *
     * Permissions are already in memory — the association is eager — so that count is
     * free. Holders are one grouped query for the whole page rather than one per row.
     */
    private List<RoleDTO> withCounts(List<Role> roles) {
        List<RoleDTO> dtos = roles.stream().map(roleGetService::convertToDTO).toList();
        if (roles.isEmpty()) return dtos;

        List<Long> ids = roles.stream().map(Role::getId).toList();
        Map<Long, Long> holders = new HashMap<>();
        try {
            for (Object[] row : userRepository.countUsersByRoleIds(ids)) {
                holders.put(((Number) row[0]).longValue(), ((Number) row[1]).longValue());
            }
        } catch (Exception e) {
            // A count we cannot get is left null, which the UI renders as "—".
            log.warn("Could not count role holders", e);
        }

        for (int i = 0; i < roles.size(); i++) {
            Role role = roles.get(i);
            RoleDTO dto = dtos.get(i);
            dto.setPermissionCount(role.getPermissions() == null ? 0 : role.getPermissions().size());
            Long count = holders.get(role.getId());
            dto.setUserCount(count == null ? 0 : count.intValue());
        }
        return dtos;
    }

    /**
     * The cards that head the list.
     *
     * Every one is reachable as a filter, and the two "rot" counters are the reason this
     * screen is worth opening: a role granting nothing and a role nobody holds both look
     * like working access until somebody counts them.
     */
    private Map<String, Object> buildStats(Specification<Role> spec) {
        return listStats.of(Role.class, spec)
            .total()
            .count("active", RoleSpecification.isActive(true))
            .complement("inactive", "active")
            .count("system", RoleSpecification.isSystemRole(true))
            .complement("custom", "system")
            .count("noPermissions", RoleSpecification.hasNoPermissions())
            .count("noUsers", RoleSpecification.hasNoUsers())
            .recency(RoleSpecification::createdAfter)
            .build();
    }
}
