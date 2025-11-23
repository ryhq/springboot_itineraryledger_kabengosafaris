package com.itineraryledger.kabengosafaris.Security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.itineraryledger.kabengosafaris.Security.SecuritySettings.SecuritySettingsGetterServices;

/**
 * Service for validating passwords against security policies stored in the database.
 * Uses SecuritySettingsService to fetch dynamic password policy rules.
 *
 * Validates:
 * - Minimum and maximum length
 * - Uppercase letters requirement
 * - Lowercase letters requirement
 * - Numbers requirement
 * - Special characters requirement
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordValidator {

    @Autowired
    private SecuritySettingsGetterServices securitySettingsGetterServices;

    /**
     * Validate password against database password security policies
     *
     * @param password the password to validate
     * @throws IllegalArgumentException if password doesn't meet security requirements
     */
    public void validatePassword(String password) {
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("Password cannot be null or empty");
        }

        try {
            // Fetch password policy settings from database
            int minLength = securitySettingsGetterServices.getPasswordMinLength();
            int maxLength = securitySettingsGetterServices.getPasswordMaxLength();
            boolean requireUppercase = securitySettingsGetterServices.getPasswordRequireUppercase();
            boolean requireLowercase = securitySettingsGetterServices.getPasswordRequireLowercase();
            boolean requireNumbers = securitySettingsGetterServices.getPasswordRequireNumbers();
            boolean requireSpecialCharacters = securitySettingsGetterServices.getPasswordRequireSpecialCharacters();

            // Validate minimum length
            if (password.length() < minLength) {
                throw new IllegalArgumentException("Password must be at least " + minLength + " characters long");
            }

            // Validate maximum length
            if (password.length() > maxLength) {
                throw new IllegalArgumentException("Password cannot exceed " + maxLength + " characters");
            }

            // Validate uppercase requirement
            if (requireUppercase && !password.matches(".*[A-Z].*")) {
                throw new IllegalArgumentException(
                        "Password must contain at least one uppercase letter"
                );
            }

            // Validate lowercase requirement
            if (requireLowercase && !password.matches(".*[a-z].*")) {
                throw new IllegalArgumentException(
                        "Password must contain at least one lowercase letter"
                );
            }

            // Validate numbers requirement
            if (requireNumbers && !password.matches(".*\\d.*")) {
                throw new IllegalArgumentException(
                        "Password must contain at least one number"
                );
            }

            // Validate special characters requirement
            if (requireSpecialCharacters && !password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};:'\",.<>?/\\\\|`~].*")) {
                throw new IllegalArgumentException(
                        "Password must contain at least one special character"
                );
            }

            log.debug("Password validation passed");

        } catch (IllegalArgumentException e) {
            log.warn("Password validation failed: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error validating password against security policies", e);
            throw new IllegalArgumentException("Failed to validate password against security policies", e);
        }
    }
}
