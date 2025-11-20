package com.itineraryledger.kabengosafaris.User.Controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.User.User;
import com.itineraryledger.kabengosafaris.User.DTOs.UserDTO;
import com.itineraryledger.kabengosafaris.User.Services.UserService;

import lombok.extern.slf4j.Slf4j;

/**
 * REST Controller for user-related endpoints
 */
@RestController
@RequestMapping("/api/users")
@Slf4j
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * Get authenticated user's details
     * Requires authentication via JWT token
     *
     * @return ResponseEntity with ApiResponse containing UserDTO
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserDTO>> getCurrentUser() {
        try {
            // Get authentication from security context
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            // Check if user is authenticated
            if (authentication == null || !authentication.isAuthenticated()) {
                log.warn("Unauthorized access attempt to /me endpoint");
                return ResponseEntity.status(
                        HttpStatus.UNAUTHORIZED
                ).body(
                        ApiResponse.error(
                                HttpStatus.UNAUTHORIZED.value(),
                                "User not authenticated",
                                "AUTHENTICATION_REQUIRED"
                        )
                );
            }

            // Get username from authentication
            String username = authentication.getName();
            log.debug("Fetching user details for username: {}", username);

            // Retrieve user from database
            User user = userService.findByUsername(username).orElse(null);

            if (user == null) {
                log.warn("User not found: {}", username);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error(
                                HttpStatus.NOT_FOUND.value(),
                                "User not found",
                                "USER_NOT_FOUND"
                        ));
            }

            // Convert to DTO with obfuscated ID
            UserDTO userDTO = userService.convertToDTO(user);

            log.info("Successfully retrieved user details for username: {}", username);
            return ResponseEntity.ok(ApiResponse.success(
                    HttpStatus.OK.value(),
                    "User details retrieved successfully",
                    userDTO
            ));

        } catch (Exception e) {
            log.error("Error retrieving user details", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(
                            HttpStatus.INTERNAL_SERVER_ERROR.value(),
                            "An error occurred while retrieving user details",
                            "INTERNAL_SERVER_ERROR"
                    ));
        }
    }
}
