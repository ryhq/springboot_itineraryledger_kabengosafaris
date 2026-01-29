package com.itineraryledger.kabengosafaris.Role.Services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Role.Role;
import com.itineraryledger.kabengosafaris.Role.RoleRepository;
import com.itineraryledger.kabengosafaris.Role.DTOs.RoleUserAssignmentDTO;
import com.itineraryledger.kabengosafaris.Role.DTOs.RoleUserDTO;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import com.itineraryledger.kabengosafaris.User.User;
import com.itineraryledger.kabengosafaris.User.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for managing user-role assignments.
 *
 * Provides methods to:
 * - Get all users with their assignment status for a specific role
 * - Batch assign or remove users from roles
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RoleUserAssignmentService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final IdObfuscator idObfuscator;

    /**
     * Get all users with their assignment status for a specific role.
     *
     * Returns paginated users with a boolean 'assigned' field indicating
     * whether each user is assigned to the specified role.
     *
     * @param roleIdObfuscated The obfuscated role ID
     * @param page Page number (0-based)
     * @param size Page size
     * @param search Optional search keyword for username, email, or name (case-insensitive)
     * @param sortDir Sort direction ("asc" or "desc")
     * @return ResponseEntity with paginated list of RoleUserDTO
     */
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<?>> getUsersForRole(
            String roleIdObfuscated,
            int page,
            int size,
            String search,
            String sortDir
    ) {
        log.info("Getting users for role: {}, page: {}, size: {}, search: {}",
            roleIdObfuscated, page, size, search);

        try {
            // Decode role ID
            Long roleId = idObfuscator.decodeId(roleIdObfuscated);
            if (roleId == null) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid role ID", "INVALID_ROLE_ID")
                );
            }

            // Find role
            Role role = roleRepository.findById(roleId).orElse(null);
            if (role == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Role not found", "ROLE_NOT_FOUND")
                );
            }

            // Validate pagination
            if (page < 0) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Page number cannot be negative", "INVALID_PAGE")
                );
            }
            if (size <= 0) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Page size must be greater than 0", "INVALID_SIZE")
                );
            }

            // Setup sorting
            Sort.Direction direction = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
            Pageable paging = PageRequest.of(page, size, Sort.by(direction, "firstName", "lastName"));

            // Build specification with search filter
            Specification<User> specification = Specification.unrestricted();
            if (search != null && !search.isBlank()) {
                specification = specification.and(UserSpecification.keywordSearch(search));
            }

            // Get all users with pagination and search
            Page<User> pagedUsers = userRepository.findAll(specification, paging);

            // Get IDs of users who have this role (from the role's perspective)
            // We need to query users who have this role assigned
            Specification<User> hasRoleSpec = UserSpecification.hasRole(roleId);
            List<User> usersWithRole = userRepository.findAll(hasRoleSpec);
            Set<Long> usersWithRoleIds = usersWithRole.stream()
                .map(User::getId)
                .collect(Collectors.toSet());

            // Convert to DTOs with assigned flag
            List<RoleUserDTO> userDTOs = pagedUsers.getContent().stream()
                .map(user -> convertToRoleUserDTO(user, usersWithRoleIds.contains(user.getId())))
                .toList();

            // Build response
            Map<String, Object> response = new HashMap<>();
            response.put("role", Map.of(
                "id", roleIdObfuscated,
                "name", role.getName(),
                "displayName", role.getDisplayName()
            ));
            response.put("users", userDTOs);
            response.put("currentPage", pagedUsers.getNumber());
            response.put("totalItems", pagedUsers.getTotalElements());
            response.put("totalPages", pagedUsers.getTotalPages());
            response.put("assignedCount", usersWithRoleIds.size());

            log.info("Retrieved {} users for role {}, {} assigned",
                userDTOs.size(), role.getName(), usersWithRoleIds.size());
            return ResponseEntity.ok(ApiResponse.success(200, "Users retrieved successfully", response));

        } catch (Exception e) {
            log.error("Failed to get users for role", e);
            return ResponseEntity.status(500).body(
                ApiResponse.error(500, "Failed to get users: " + e.getMessage(), "GET_USERS_FAILED")
            );
        }
    }

    /**
     * Batch assign or remove users from roles.
     *
     * Each item in the list specifies a user, role, and whether to assign or remove.
     *
     * @param requests List of assignment requests
     * @return ResponseEntity with results for each operation
     */
    @Transactional
    @AuditLogAnnotation(
        action = "BATCH_USER_ROLE_ASSIGNMENT",
        description = "Batch assigning or removing user roles",
        entityType = "Role"
    )
    public ResponseEntity<ApiResponse<?>> assignUserRoles(List<RoleUserAssignmentDTO> requests) {
        log.info("Processing batch user-role assignment: {} requests", requests.size());

        if (requests == null || requests.isEmpty()) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(400, "Request list cannot be empty", "EMPTY_REQUEST")
            );
        }

        try {
            List<Map<String, Object>> results = new ArrayList<>();
            int successCount = 0;
            int failCount = 0;

            for (RoleUserAssignmentDTO request : requests) {
                Map<String, Object> result = processAssignment(request);
                results.add(result);

                if (Boolean.TRUE.equals(result.get("success"))) {
                    successCount++;
                } else {
                    failCount++;
                }
            }

            // Build response
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("results", results);
            responseData.put("totalProcessed", requests.size());
            responseData.put("successCount", successCount);
            responseData.put("failCount", failCount);

            String message = String.format("Processed %d assignments: %d succeeded, %d failed",
                requests.size(), successCount, failCount);

            log.info(message);
            return ResponseEntity.ok(ApiResponse.success(200, message, responseData));

        } catch (Exception e) {
            log.error("Failed to process batch user-role assignment", e);
            return ResponseEntity.status(500).body(
                ApiResponse.error(500, "Failed to process assignments: " + e.getMessage(), "ASSIGNMENT_FAILED")
            );
        }
    }

    /**
     * Process a single assignment request.
     *
     * @param request The assignment request
     * @return Map with result details
     */
    private Map<String, Object> processAssignment(RoleUserAssignmentDTO request) {
        Map<String, Object> result = new HashMap<>();
        result.put("userId", request.getUserId());
        result.put("roleId", request.getRoleId());
        result.put("requestedAction", request.getAssign() ? "assign" : "remove");

        try {
            // Decode user ID
            Long userId = idObfuscator.decodeId(request.getUserId());
            if (userId == null) {
                result.put("success", false);
                result.put("error", "Invalid user ID");
                result.put("errorCode", "INVALID_USER_ID");
                return result;
            }

            // Decode role ID
            Long roleId = idObfuscator.decodeId(request.getRoleId());
            if (roleId == null) {
                result.put("success", false);
                result.put("error", "Invalid role ID");
                result.put("errorCode", "INVALID_ROLE_ID");
                return result;
            }

            // Find user
            User user = userRepository.findById(userId).orElse(null);
            if (user == null) {
                result.put("success", false);
                result.put("error", "User not found");
                result.put("errorCode", "USER_NOT_FOUND");
                return result;
            }

            // Find role
            Role role = roleRepository.findById(roleId).orElse(null);
            if (role == null) {
                result.put("success", false);
                result.put("error", "Role not found");
                result.put("errorCode", "ROLE_NOT_FOUND");
                return result;
            }

            // Check if role is active (only active roles can be assigned)
            if (request.getAssign() && !role.getActive()) {
                result.put("success", false);
                result.put("error", "Cannot assign inactive role: " + role.getDisplayName());
                result.put("errorCode", "INACTIVE_ROLE");
                return result;
            }

            boolean currentlyAssigned = user.getRoles().stream()
                .anyMatch(r -> r.getId().equals(roleId));

            if (request.getAssign()) {
                // Assign role
                if (currentlyAssigned) {
                    result.put("success", true);
                    result.put("message", "User already has role: " + role.getDisplayName());
                    result.put("action", "no_change");
                } else {
                    user.getRoles().add(role);
                    userRepository.save(user);
                    result.put("success", true);
                    result.put("message", "Role assigned successfully");
                    result.put("action", "assigned");
                    log.debug("Assigned role {} to user {}", role.getName(), user.getUsername());
                }
            } else {
                // Remove role
                if (!currentlyAssigned) {
                    result.put("success", true);
                    result.put("message", "User does not have role: " + role.getDisplayName());
                    result.put("action", "no_change");
                } else {
                    user.getRoles().removeIf(r -> r.getId().equals(roleId));
                    userRepository.save(user);
                    result.put("success", true);
                    result.put("message", "Role removed successfully");
                    result.put("action", "removed");
                    log.debug("Removed role {} from user {}", role.getName(), user.getUsername());
                }
            }

            result.put("roleName", role.getName());
            result.put("roleDisplayName", role.getDisplayName());
            result.put("userName", user.getUsername());
            result.put("userFullName", user.getFirstName() + " " + user.getLastName());
            result.put("assigned", request.getAssign() ? currentlyAssigned || !currentlyAssigned : !currentlyAssigned && currentlyAssigned);
            // Simplified: after operation, is the user assigned?
            result.put("currentlyAssigned", request.getAssign() || (!request.getAssign() && !currentlyAssigned));

        } catch (Exception e) {
            log.error("Failed to process assignment for user {} and role {}",
                request.getUserId(), request.getRoleId(), e);
            result.put("success", false);
            result.put("error", "Processing error: " + e.getMessage());
            result.put("errorCode", "PROCESSING_ERROR");
        }

        return result;
    }

    /**
     * Convert User entity to RoleUserDTO.
     */
    private RoleUserDTO convertToRoleUserDTO(User user, boolean assigned) {
        return RoleUserDTO.builder()
            .id(idObfuscator.encodeId(user.getId()))
            .username(user.getUsername())
            .email(user.getEmail())
            .firstName(user.getFirstName())
            .lastName(user.getLastName())
            .fullName(user.getFirstName() + " " + user.getLastName())
            .enabled(user.getEnabled())
            .assigned(assigned)
            .build();
    }
}
