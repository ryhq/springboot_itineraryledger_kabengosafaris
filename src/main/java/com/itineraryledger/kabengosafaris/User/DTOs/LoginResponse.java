package com.itineraryledger.kabengosafaris.User.DTOs;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object for successful login response.
 * Contains access token, refresh token, and user information.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginResponse {
    /**
     * JWT access token for API authentication
     */
    private String accessToken;

    /**
     * JWT refresh token for obtaining new access tokens
     */
    private String refreshToken;

    /**
     * Access token expiration time in milliseconds
     */
    private Long accessTokenExpiresIn;

    /**
     * Refresh token expiration time in milliseconds
     */
    private Long refreshTokenExpiresIn;

    /**
     * Access token expiration time in YYYY-MM-DD HH:MM:SS format
     */
    private String accessTokenExpiresAt;

    /**
     * Refresh token expiration time in YYYY-MM-DD HH:MM:SS format
     */
    private String refreshTokenExpiresAt;

}
