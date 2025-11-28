package com.itineraryledger.kabengosafaris.User.Services;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import com.itineraryledger.kabengosafaris.User.User;
import com.itineraryledger.kabengosafaris.User.UserRepository;
import com.itineraryledger.kabengosafaris.User.DTOs.UserDTO;

import lombok.extern.slf4j.Slf4j;

/**
 * Service for user-related operations
 */
@Service
@Slf4j
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private IdObfuscator idObfuscator;

    /**
     * Get user by username
     * @param username the username to search for
     * @return Optional containing the User if found
     */
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    /**
     * Get user by email
     * @param email the email to search for
     * @return Optional containing the User if found
     */
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    /**
     * Convert User entity to UserDTO with obfuscated ID
     * @param user the User entity
     * @return UserDTO with obfuscated ID and sensitive fields excluded
     */
    public UserDTO convertToDTO(User user) {
        if (user == null) {
            return null;
        }

        UserDTO dto = new UserDTO();
        // Use obfuscated ID instead of numeric ID
        dto.setId(idObfuscator.encodeId(user.getId()));

        // Personal Info
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setUsername(user.getUsername());
        dto.setBio(user.getBio());
        dto.setProfilePictureUrl(user.getProfilePictureUrl());

        // Contact Info
        dto.setEmail(user.getEmail());
        dto.setPhoneNumber(user.getPhoneNumber());

        // Account Info
        dto.setEnabled(user.getEnabled());
        dto.setAccountLocked(user.getAccountLocked());
        dto.setCreatedAt(user.getCreatedAt());
        dto.setUpdatedAt(user.getUpdatedAt());

        // Password & MFA (Security)
        dto.setFailedAttempt(user.getFailedAttempt());
        dto.setLastFailedAttemptTime(user.getLastFailedAttemptTime());
        dto.setAccountLockedTime(user.getAccountLockedTime());
        dto.setPasswordExpiryDate(user.getPasswordExpiryDate());

        dto.setMfaEnabled(user.isMfaEnabled());
        dto.setMfaEnabledAt(user.getMfaEnabledAt());
        dto.setMfaConfirmed(user.getMfaConfirmed());
        return dto;
    }

    public void saveUser(User user) {
        userRepository.save(user);
    }

    public ResponseEntity<?> getMe(Authentication authentication) {
        try {
            // Check if user is authenticated
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
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
            User user = findByUsername(username).orElse(null);

            if (user == null) {
                log.warn("User not found: {}", username);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(
                    HttpStatus.NOT_FOUND.value(),
                    "User not found",
                    "USER_NOT_FOUND"
                ));
            }

            // Convert to DTO with obfuscated ID
            UserDTO userDTO = convertToDTO(user);

            log.info("Successfully retrieved user details for username: {}", username);
            return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.OK.value(),
                "User details retrieved successfully",
                userDTO
            ));
        } catch (Exception e) {
            log.error("Error retrieving user details", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "An error occurred while retrieving user details",
                    "INTERNAL_SERVER_ERROR"
                )
            );
        }

    }
}
