package com.itineraryledger.kabengosafaris.User.Services.UserUpdateServices;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.PasswordHasher;
import com.itineraryledger.kabengosafaris.Security.PasswordValidator;
import com.itineraryledger.kabengosafaris.Security.SecuritySettings.SecuritySettingsGetterServices;
import com.itineraryledger.kabengosafaris.User.User;
import com.itineraryledger.kabengosafaris.User.UserRepository;
import com.itineraryledger.kabengosafaris.User.DTOs.UpdatePasswordRequest;
import com.itineraryledger.kabengosafaris.User.DTOs.UpdateUserProfileRequest;
import com.itineraryledger.kabengosafaris.User.DTOs.UserDTO;
import com.itineraryledger.kabengosafaris.User.Services.UserService;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class UserUpdateServices {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordValidator passwordValidator;

    @Autowired
    private SecuritySettingsGetterServices securitySettingsGetterServices;

    
    @AuditLogAnnotation(action = "UPDATE_PROFILE", description = "User self profile update", entityType = "User")
    public ResponseEntity<?> updatePersonalDetails(
        Authentication authentication, 
        UpdateUserProfileRequest updateRequest
    ) {
        if (updateRequest == null) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(
                    400, 
                    "Update request cannot be null", 
                "VALIDATION_ERROR"
                )
            );
        }

        // First name validation
        if (updateRequest.getFirstName() == null || updateRequest.getFirstName().isBlank()) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(
                    400, 
                    "First name is required", 
                "VALIDATION_ERROR"
                )
            );
        }

        // Last name validation
        if (updateRequest.getLastName() == null || updateRequest.getLastName().isBlank()) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(
                    400, 
                    "Last name is required", 
                "VALIDATION_ERROR"
                )
            );
        }

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
        User user = userService.findByUsername(username).orElse(null);

        if (user == null) {
            log.warn("User not found: {}", username);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(
                    HttpStatus.NOT_FOUND.value(),
                    "User not found",
                    "USER_NOT_FOUND"
            ));
        }

        // Update profile information
        user.setFirstName(updateRequest.getFirstName().trim());
        user.setLastName(updateRequest.getLastName().trim());

        if (updateRequest.getPhoneNumber() != null && !updateRequest.getPhoneNumber().isBlank()) {
            // Check if phone number is already in use by another user
            Optional<User> existingPhoneUser = userRepository.findByPhoneNumber(updateRequest.getPhoneNumber().trim());
            if (existingPhoneUser.isPresent() && !existingPhoneUser.get().getId().equals(user.getId())) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(
                    ApiResponse.error(
                        400, 
                        "Phone number already in use", 
                        "CONFLICT_ERROR"
                    )
                );
            }
            user.setPhoneNumber(updateRequest.getPhoneNumber().trim());
        }

        // Save and return updated user
        userRepository.save(user);
        log.info("User profile updated for username: {}", username);

        UserDTO userDTO = userService.convertToDTO(user);

        return ResponseEntity.ok(
            ApiResponse.success(200, "User profile upated successfully", userDTO)
        );
    }

    @AuditLogAnnotation(action = "UPDATE_PASSWORD", description = "User self password update", entityType = "User")
    public ResponseEntity<?> updatePassword(
        Authentication authentication,
        UpdatePasswordRequest updateRequest
    ) {
        if (updateRequest == null) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(
                    400,
                    "Update request cannot be null",
                    "VALIDATION_ERROR"
                )
            );
        }

        // Password validation
        if (updateRequest.getNewPassword() == null || updateRequest.getNewPassword().isBlank()) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(
                    400,
                    "New password is required",
                    "VALIDATION_ERROR"
                )
            );
        }

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
        log.debug("Updating password for username: {}", username);

        // Retrieve user from database
        User user = userService.findByUsername(username).orElse(null);

        if (user == null) {
            log.warn("User not found: {}", username);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                ApiResponse.error(
                    HttpStatus.NOT_FOUND.value(),
                    "User not found",
                    "USER_NOT_FOUND"
                )
            );
        }

        // Validate new password against password policy
        try {
            passwordValidator.validatePassword(updateRequest.getNewPassword());
        } catch (IllegalArgumentException e) {
            log.warn("Password validation failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(
                ApiResponse.error(
                    400,
                    e.getMessage(),
                    "PASSWORD_POLICY_VIOLATION"
                )
            );
        }

        // Hash the new password
        String hashedPassword = PasswordHasher.hashPassword(updateRequest.getNewPassword());
        user.setPassword(hashedPassword);

        // Reset password expiry date from database settings
        try {
            int expirationDays = securitySettingsGetterServices.getPasswordExpirationDays();
            if (expirationDays > 0) {
                user.setPasswordExpiryDate(LocalDateTime.now().plusDays(expirationDays));
            } else {
                user.setPasswordExpiryDate(null);
            }
        } catch (Exception e) {
            log.warn("Could not update password expiry date: {}", e.getMessage());
        }

        // Save and return updated user
        userRepository.save(user);
        log.info("Password updated for username: {}", username);

        UserDTO userDTO = userService.convertToDTO(user);

        return ResponseEntity.ok(
            ApiResponse.success(200, "Password updated successfully", userDTO)
        );
    }
}
