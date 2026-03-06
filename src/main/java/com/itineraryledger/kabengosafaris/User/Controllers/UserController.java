package com.itineraryledger.kabengosafaris.User.Controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.itineraryledger.kabengosafaris.Role.Services.RoleGetService;
import com.itineraryledger.kabengosafaris.User.DTOs.UpdatePasswordRequest;
import com.itineraryledger.kabengosafaris.User.DTOs.UpdateUserProfileRequest;
import com.itineraryledger.kabengosafaris.User.User;
import com.itineraryledger.kabengosafaris.User.Services.UserService;
import com.itineraryledger.kabengosafaris.User.Services.UserUpdateServices.UserUpdateServices;

import lombok.extern.slf4j.Slf4j;

/**
 * REST Controller for user self-service endpoints
 *
 * All endpoints in this controller are restricted to the currently authenticated user.
 * Users can only view and modify their own data through these endpoints.
 */
@RestController
@RequestMapping("/api/user/me")
@Slf4j
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private UserUpdateServices userUpdateServices;

    @Autowired
    private RoleGetService roleGetService;

    /**
     * Get current authenticated user's details
     *
     * @param authentication The authentication object containing current user details
     * @return ResponseEntity with current user's information
     *
     * Security: Requires authenticated user (any logged-in user can access their own data)
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getCurrentUser(Authentication authentication) {
        return userService.getMe(authentication);
    }

    /**
     * Get current authenticated user's roles with optional filtering and pagination
     *
     * @param authentication The authentication object containing current user details
     * @param page Page number (0-based), default: 0
     * @param size Page size, default: 10
     * @param name Filter by role name partial match (optional)
     * @param displayName Filter by display name partial match (optional)
     * @param active Filter by active status (optional)
     * @param isSystemRole Filter by system role status (optional)
     * @param sortDir Sort direction: "asc" or "desc", default: "desc"
     * @return ResponseEntity with paginated roles assigned to current user
     *
     * Security: Requires authenticated user (users can only view their own roles)
     *
     * Example: GET /api/user/me/roles?page=0&size=10&active=true&sortDir=desc
     */
    @GetMapping("/roles")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getCurrentUserRoles(
        Authentication authentication,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size,
        @RequestParam(required = false) String name,
        @RequestParam(required = false) String displayName,
        @RequestParam(required = false) Boolean active,
        @RequestParam(required = false) Boolean isSystemRole,
        @RequestParam(required = false) String sortBy,
        @RequestParam(required = false) String sortDir
    ) {
        // Get the current user from authentication
        User currentUser = (User) authentication.getPrincipal();
        Long userId = currentUser.getId();

        log.info("Fetching roles for current user: {} (ID: {})", currentUser.getUsername(), userId);

        // Delegate to RoleGetService with the user filter
        return roleGetService.getRolesForUser(
            userId,
            page,
            size,
            name,
            displayName,
            active,
            isSystemRole,
            sortBy,
            sortDir
        );
    }

    /**
     * Update current user's personal details
     *
     * @param authentication The authentication object containing current user details
     * @param updateRequest The request DTO with personal details to update
     * @return ResponseEntity with updated user information
     *
     * Security: Requires authenticated user (users can only update their own personal details)
     */
    @PutMapping("/personal-details")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> updatePersonalDetails(Authentication authentication, @RequestBody UpdateUserProfileRequest updateRequest) {
        return userUpdateServices.updatePersonalDetails(authentication, updateRequest);
    }

    /**
     * Update current user's password
     *
     * @param authentication The authentication object containing current user details
     * @param updateRequest The request DTO with old and new password
     * @return ResponseEntity with password update result
     *
     * Security: Requires authenticated user (users can only change their own password)
     */
    @PutMapping("/password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> updatePassword(Authentication authentication, @RequestBody UpdatePasswordRequest updateRequest) {
        return userUpdateServices.updatePassword(authentication, updateRequest);
    }
}
