package com.itineraryledger.kabengosafaris.User.DTOs;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object for User
 * This DTO excludes sensitive fields like password for API responses.
 * IDs are obfuscated using IdObfuscator for security.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserDTO {
    // Obfuscated ID
    private String id; 
    
    // Personal Info
    private String firstName;
    private String lastName;
    private String username;
    private String bio;
    private String profilePictureUrl;
    
    // Contact Info
    private String email;
    private String phoneNumber;
    
    // Account Info
    private Boolean enabled;
    private Boolean accountLocked;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Password & MFA (Security)
    private int failedAttempt; // Failed login Attempts
    private LocalDateTime lastFailedAttemptTime;
    private LocalDateTime accountLockedTime;
    private LocalDateTime passwordExpiryDate;

    private boolean mfaEnabled;
    private LocalDateTime mfaEnabledAt;
    private Boolean mfaConfirmed = false; 
    private LocalDateTime lastMfaVerification;
    private String mfaSecret;
}
