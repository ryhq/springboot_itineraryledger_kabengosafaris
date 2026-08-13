package com.itineraryledger.kabengosafaris.User.Services.UserAdminServices;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
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
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import com.itineraryledger.kabengosafaris.User.User;
import com.itineraryledger.kabengosafaris.User.UserRepository;
import com.itineraryledger.kabengosafaris.User.DTOs.UserDTO;
import com.itineraryledger.kabengosafaris.User.DTOs.UserRoleRefDTO;
import com.itineraryledger.kabengosafaris.User.Specifications.UserFilter;
import com.itineraryledger.kabengosafaris.User.Specifications.UserSpecification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Reading the accounts, for an administrator rather than for oneself.
 *
 * UserService answers "who am I"; this answers "who can get in, and what can they
 * do". The counters are the point as much as the rows: an account with no role, a
 * lockout from this morning and an invite nobody ever accepted are all invisible
 * until something counts them.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class UserAdminGetService {

    private static final List<String> VALID_SORT_FIELDS = Arrays.asList(
        "firstName", "lastName", "username", "email", "enabled", "createdAt", "updatedAt");
    private static final String DEFAULT_SORT_FIELD = "firstName";

    private final UserRepository userRepository;
    private final IdObfuscator idObfuscator;
    private final ListStats listStats;
    private final RecordNavigation recordNavigation;

    public ResponseEntity<ApiResponse<?>> list(
        UserFilter filter,
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
            // clamp: an unbounded size is a way to ask for the whole table by accident
            int pageSize = size == null || size <= 0 ? 20 : Math.min(size, 100);
            Pageable pageable = PageRequest.of(
                page == null || page < 0 ? 0 : page, pageSize, Sort.by(direction, resolvedSort));

            Specification<User> spec = buildSpec(filter != null ? filter : new UserFilter());
            Page<User> found = userRepository.findAll(spec, pageable);

            Map<String, Object> response = new HashMap<>();
            response.put("users", found.getContent().stream().map(this::toDTO).toList());
            response.put("currentPage", found.getNumber());
            response.put("totalItems", found.getTotalElements());
            response.put("totalPages", found.getTotalPages());
            response.put("pageSize", found.getSize());
            response.put("validSortFields", VALID_SORT_FIELDS);
            response.put("currentSortBy", resolvedSort);
            response.put("currentSortDirection", direction.name().toLowerCase());
            /*
             * Counters over the WHOLE filtered set, from the same specification as the
             * rows, so a card and the table under it cannot disagree.
             */
            if (!Boolean.FALSE.equals(includeStats)) {
                response.put("stats", buildStats(spec));
            }

            return ResponseEntity.ok(ApiResponse.success(200, "Users retrieved", response));
        } catch (Exception e) {
            log.error("Error listing users", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to list users", "USERS_LIST_FAILED"));
        }
    }

    /** One account, and where it sits in the set the caller came from. */
    public ResponseEntity<ApiResponse<?>> getOne(
        String idObfuscated,
        UserFilter filter,
        String sortBy,
        String sortDirection
    ) {
        try {
            Long id = idObfuscator.decodeId(idObfuscated);
            User user = userRepository.findById(id).orElse(null);
            if (user == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "User not found", "USER_NOT_FOUND"));
            }

            Specification<User> navSpec = buildSpec(filter != null ? filter : new UserFilter());
            String navSortBy = sortBy != null && VALID_SORT_FIELDS.contains(sortBy)
                ? sortBy : DEFAULT_SORT_FIELD;
            Map<String, Object> nav = recordNavigation.navigate(
                User.class, navSpec, navSortBy, !"desc".equalsIgnoreCase(sortDirection), id);

            Long nextId = (Long) nav.get("nextRawId");
            Long previousId = (Long) nav.get("previousRawId");

            Map<String, Object> response = new HashMap<>();
            response.put("user", toDTO(user));
            response.put("nextId", nextId != null ? idObfuscator.encodeId(nextId) : null);
            response.put("previousId", previousId != null ? idObfuscator.encodeId(previousId) : null);
            response.put("position", nav.get("position"));
            response.put("total", nav.get("total"));

            return ResponseEntity.ok(ApiResponse.success(200, "User retrieved", response));
        } catch (Exception e) {
            log.error("Error fetching user", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to fetch user", "USER_FETCH_FAILED"));
        }
    }

    /** ONE specification, shared by the rows, the counters and the record walk. */
    public Specification<User> buildSpec(UserFilter filter) {
        Specification<User> spec = Specification.<User>unrestricted()
            .and(UserSpecification.searchKeyword(filter.effectiveKeyword()))
            .and(UserSpecification.hasMfa(filter.getMfaEnabled()))
            .and(UserSpecification.byRoleNames(filter.getRoleNames()))
            .and(UserSpecification.createdAfter(atStartOfDay(filter.getCreatedAfter())))
            .and(UserSpecification.createdBefore(atEndOfDay(filter.getCreatedBefore())));

        List<Long> roleIds = decodeAll(filter.allRoleIds());
        if (roleIds != null) spec = spec.and(UserSpecification.byRoleIds(roleIds));

        /*
         * The status dimension. active/inactive is the enabled flag; locked is its own
         * thing, because a locked account is still enabled and simply cannot get in.
         * Contradictory pairs cancel to no constraint, as everywhere else — asking for
         * active AND inactive is asking for both, not for nothing.
         */
        boolean wantsActive = filter.hasStatus("active");
        boolean wantsInactive = filter.hasStatus("inactive");
        if (wantsActive != wantsInactive) {
            spec = spec.and(UserSpecification.isEnabled(wantsActive));
        } else if (filter.getEnabled() != null) {
            spec = spec.and(UserSpecification.isEnabled(filter.getEnabled()));
        }

        if (filter.hasStatus("locked")) {
            spec = spec.and(UserSpecification.isLocked(true));
        } else if (filter.getAccountLocked() != null) {
            spec = spec.and(UserSpecification.isLocked(filter.getAccountLocked()));
        }

        // what needs attention, OR'd together
        Specification<User> quality = null;
        if (filter.wants("noRoles")) quality = or(quality, UserSpecification.hasNoRoles());
        if (filter.wants("neverSignedIn")) quality = or(quality, UserSpecification.neverSignedIn());
        if (filter.wants("mfaOff")) quality = or(quality, UserSpecification.hasMfa(false));
        if (filter.wants("passwordExpired")) quality = or(quality, UserSpecification.passwordExpired());
        if (filter.wants("failedAttempts")) quality = or(quality, UserSpecification.hasFailedAttempts());
        if (quality != null) spec = spec.and(quality);

        return spec;
    }

    private Specification<User> or(Specification<User> spec, Specification<User> extra) {
        return spec == null ? extra : spec.or(extra);
    }

    /**
     * Decodes the obfuscated ids, or narrows to nothing if none of them read.
     *
     * An unreadable id must not quietly widen the list to every account — that is the
     * opposite of what was asked for, and on this screen it is the access list.
     */
    private List<Long> decodeAll(List<String> obfuscated) {
        if (obfuscated == null || obfuscated.isEmpty()) return null;
        List<Long> ids = new ArrayList<>();
        for (String value : obfuscated) {
            try {
                ids.add(idObfuscator.decodeId(value));
            } catch (Exception e) {
                log.warn("Unreadable role id on the users filter: {}", value);
            }
        }
        // an impossible id, so the caller sees an empty list rather than everybody
        if (ids.isEmpty()) ids.add(-1L);
        return ids;
    }

    private LocalDateTime atStartOfDay(LocalDate date) {
        return date == null ? null : date.atStartOfDay();
    }

    private LocalDateTime atEndOfDay(LocalDate date) {
        return date == null ? null : date.atTime(23, 59, 59);
    }

    /**
     * The cards that head the list.
     *
     * Every one of these is reachable as a filter, and every filter here has a card —
     * a counter nobody can click through to only tells you that you have a problem.
     */
    private Map<String, Object> buildStats(Specification<User> spec) {
        return listStats.of(User.class, spec)
            .total()
            .count("active", UserSpecification.isEnabled(true))
            .complement("inactive", "active")
            .count("locked", UserSpecification.isLocked(true))
            .count("noRoles", UserSpecification.hasNoRoles())
            .count("neverSignedIn", UserSpecification.neverSignedIn())
            .count("mfaOn", UserSpecification.hasMfa(true))
            .count("mfaOff", UserSpecification.hasMfa(false))
            .count("passwordExpired", UserSpecification.passwordExpired())
            .count("failedAttempts", UserSpecification.hasFailedAttempts())
            .recency(UserSpecification::createdAfter)
            .build();
    }

    /**
     * The wire shape.
     *
     * mfaSecret is never set here. It is the TOTP seed, and putting it on a list any
     * holder of READ_USER can fetch would turn reading into impersonating.
     */
    public UserDTO toDTO(User user) {
        if (user == null) return null;

        UserDTO dto = new UserDTO();
        dto.setId(idObfuscator.encodeId(user.getId()));

        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setUsername(user.getUsername());
        dto.setBio(user.getBio());
        dto.setProfilePictureUrl(user.getProfilePictureUrl());

        dto.setEmail(user.getEmail());
        dto.setPhoneNumber(user.getPhoneNumber());

        dto.setEnabled(user.getEnabled());
        dto.setAccountLocked(user.getAccountLocked());
        dto.setCreatedAt(user.getCreatedAt());
        dto.setUpdatedAt(user.getUpdatedAt());

        dto.setFailedAttempt(user.getFailedAttempt());
        dto.setLastFailedAttemptTime(user.getLastFailedAttemptTime());
        dto.setAccountLockedTime(user.getAccountLockedTime());
        dto.setPasswordExpiryDate(user.getPasswordExpiryDate());

        dto.setMfaEnabled(user.isMfaEnabled());
        dto.setMfaEnabledAt(user.getMfaEnabledAt());
        dto.setMfaConfirmed(user.getMfaConfirmed());
        dto.setLastMfaVerification(user.getLastMfaVerification());

        List<UserRoleRefDTO> roles = new ArrayList<>();
        if (user.getRoles() != null) {
            user.getRoles().stream()
                // stable order, or the badges reshuffle between requests
                .sorted(Comparator.comparing(Role::getDisplayName, Comparator.nullsLast(String::compareTo)))
                .forEach(role -> roles.add(UserRoleRefDTO.builder()
                    .id(idObfuscator.encodeId(role.getId()))
                    .name(role.getName())
                    .displayName(role.getDisplayName())
                    .active(role.getActive())
                    .isSystemRole(role.getIsSystemRole())
                    .build()));
        }
        dto.setRoles(roles);
        dto.setRoleCount(roles.size());

        return dto;
    }
}
