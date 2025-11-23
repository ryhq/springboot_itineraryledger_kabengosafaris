package com.itineraryledger.kabengosafaris.User.Services.RegistrationServices;

import com.itineraryledger.kabengosafaris.Security.PasswordHasher;
import com.itineraryledger.kabengosafaris.Security.PasswordValidator;
import com.itineraryledger.kabengosafaris.Security.SecuritySettings.SecuritySettingsGetterServices;
import com.itineraryledger.kabengosafaris.User.User;
import com.itineraryledger.kabengosafaris.User.UserRepository;
import com.itineraryledger.kabengosafaris.User.DTOs.RegistrationRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.regex.Pattern;

/**
 * Service for handling user registration with comprehensive validation.
 * - Validates required fields (email, username, password, firstName, lastName)
 * - Enforces password policy from database via PasswordValidator
 * - Prevents duplicate email/username registration
 * - Hashes passwords using BCrypt
 */
@Service
public class RegistrationServices {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordValidator passwordValidator;

    @Autowired
    private SecuritySettingsGetterServices securitySettingsGetterServices;

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );

    /**
     * Register a new user with validation
     *
     * @param request Registration request containing user details
     * @return The registered User entity
     * @throws RegistrationException if validation fails or user already exists
     */
    public User registerUser(RegistrationRequest request) {
        // Validate all required fields
        validateRegistrationRequest(request);

        // Check if user already exists
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RegistrationException("Email already registered");
        }

        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new RegistrationException("Username already exists");
        }

        if (userRepository.findByPhoneNumber(request.getPhoneNumber()).isPresent()) {
            throw new RegistrationException("Phone already exists");
        }

        // Validate password policy using database settings
        try {
            passwordValidator.validatePassword(request.getPassword());
        } catch (IllegalArgumentException e) {
            throw new RegistrationException(e.getMessage());
        }

        // Hash the password
        String hashedPassword = PasswordHasher.hashPassword(request.getPassword());

        // Create and configure user entity
        User user = User.builder()
                .email(request.getEmail().toLowerCase().trim())
                .username(request.getUsername().trim())
                .password(hashedPassword)
                .firstName(request.getFirstName().trim())
                .lastName(request.getLastName().trim())
                .phoneNumber(request.getPhoneNumber() != null ? request.getPhoneNumber().trim() : null)
                .enabled(false) // Disabled until email verification
                .accountLocked(false)
                .failedAttempt(0)
                .build();

        // Calculate password expiry date from database settings
        try {
            int expirationDays = securitySettingsGetterServices.getPasswordExpirationDays();
            if (expirationDays > 0) {
                user.setPasswordExpiryDate(LocalDateTime.now().plusDays(expirationDays));
            }
        } catch (Exception e) {
            // Log but don't fail registration if we can't fetch password expiration setting
            // User will have no expiration date in this case
        }

        // Save and return the user
        return userRepository.save(user);
    }

    /**
     * Validate all required registration fields
     */
    private void validateRegistrationRequest(RegistrationRequest request) {
        if (request == null) {
            throw new RegistrationException("Registration request cannot be null");
        }

        // Email validation
        if (request.getEmail() == null || request.getEmail().isBlank()) {
            throw new RegistrationException("Email is required");
        }
        if (!EMAIL_PATTERN.matcher(request.getEmail()).matches()) {
            throw new RegistrationException("Invalid email format");
        }

        // Username validation
        if (request.getUsername() == null || request.getUsername().isBlank()) {
            throw new RegistrationException("Username is required");
        }
        if (request.getUsername().length() < 3) {
            throw new RegistrationException("Username must be at least 3 characters");
        }

        // Password validation
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new RegistrationException("Password is required");
        }

        // First name validation
        if (request.getFirstName() == null || request.getFirstName().isBlank()) {
            throw new RegistrationException("First name is required");
        }

        // Last name validation
        if (request.getLastName() == null || request.getLastName().isBlank()) {
            throw new RegistrationException("Last name is required");
        }
    }

}
