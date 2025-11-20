package com.itineraryledger.kabengosafaris.User.DTOs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object for login request.
 * Contains username/email and password for authentication.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {
    /**
     * Username or email address for login
     */
    private String identifier;

    /**
     * Plain text password (will be hashed for comparison)
     */
    private String password;
}
