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

    /**
     * NEVER populate this on any list or admin response.
     *
     * It is the TOTP seed. Anyone holding it can generate that account's codes
     * indefinitely, so handing it out alongside READ_USER would turn a read
     * permission into the ability to sign in as anybody. It stays on the DTO only
     * because MFA setup needs it for the enrolment QR, and @JsonInclude(NON_NULL)
     * keeps it off the wire everywhere it is left unset.
     */
    private String mfaSecret;

    /**
     * Access — which roles this account holds.
     *
     * The whole reason to look at a user is to answer "what can they do", and the
     * answer is their roles; a user list without them is a staff directory.
     */
    private java.util.List<UserRoleRefDTO> roles;
    private Integer roleCount;
}
