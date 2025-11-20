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
    private String id; // Obfuscated ID
    private String email;
    private String firstName;
    private String lastName;
    private String username;
    private String phoneNumber;
    private Boolean enabled;
    private Boolean accountLocked;
    private String bio;
    private String profilePictureUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
