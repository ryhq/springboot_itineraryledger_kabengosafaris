package com.itineraryledger.kabengosafaris.User.Services.RegistrationServices;

import com.itineraryledger.kabengosafaris.EmailAccount.EmailAccountServices.EmailSendingService;
import com.itineraryledger.kabengosafaris.EmailEvent.Services.EmailTemplateRenderer;
import com.itineraryledger.kabengosafaris.Security.JwtTokenProvider;
import com.itineraryledger.kabengosafaris.Security.PasswordHasher;
import com.itineraryledger.kabengosafaris.Security.PasswordValidator;
import com.itineraryledger.kabengosafaris.Security.SecuritySettings.SecuritySettingsGetterServices;
import com.itineraryledger.kabengosafaris.User.User;
import com.itineraryledger.kabengosafaris.User.UserRepository;
import com.itineraryledger.kabengosafaris.User.DTOs.RegistrationRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import lombok.extern.slf4j.Slf4j;

/**
 * Service for handling user registration with comprehensive validation.
 * - Validates required fields (email, username, password, firstName, lastName)
 * - Enforces password policy from database via PasswordValidator
 * - Prevents duplicate email/username registration
 * - Hashes passwords using BCrypt
 * - Sends registration email asynchronously with activation link
 */
@Service
@Slf4j
public class RegistrationServices {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordValidator passwordValidator;

    @Autowired
    private SecuritySettingsGetterServices securitySettingsGetterServices;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private EmailTemplateRenderer emailTemplateRenderer;

    @Autowired
    private EmailSendingService emailSendingService;

    @Value("${app.base.url}")
    private String appBaseUrl;

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

        if (request.getPhoneNumber() != null) {
            if (userRepository.findByPhoneNumber(request.getPhoneNumber()).isPresent()) {
                throw new RegistrationException("Phone already exists");
            }
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

        // Save the user
        User savedUser = userRepository.save(user);

        // Send registration email with activation link (asynchronously)
        // Note: This runs in the background and won't block the registration response
        // Any email sending errors will be logged but won't fail the registration
        sendRegistrationEmail(savedUser);

        return savedUser;
    }

    /**
     * Send registration email with activation token and link
     * This method is called asynchronously to avoid blocking the registration process
     *
     * @param user The newly registered user
     */
    private void sendRegistrationEmail(User user) {
        try {
            log.info("Preparing to send registration email to user: {} ({})", user.getUsername(), user.getEmail());

            // Generate activation token
            String activationToken = jwtTokenProvider.generateRegistrationTokenFromUsername(user.getUsername());

            // Build activation link
            String activationLink = appBaseUrl + "/api/auth/activate?token=" + activationToken;

            // Calculate expiration time
            Long expirationMinutes = securitySettingsGetterServices.getRegistrationJwtExpirationMinutes();
            LocalDateTime expirationDateTime = LocalDateTime.now().plusMinutes(expirationMinutes);
            Long expirationHours = expirationMinutes / 60;

            log.debug("Activation link generated for user {}: {} (expires in {} hours)",
                user.getUsername(), activationLink, expirationHours);

            // Prepare template variables
            Map<String, String> variables = new HashMap<>();
            variables.put("username", user.getUsername());
            variables.put("email", user.getEmail());
            variables.put("firstName", user.getFirstName());
            variables.put("lastName", user.getLastName());
            variables.put("phoneNumber", user.getPhoneNumber() != null ? user.getPhoneNumber() : "");
            variables.put("enabled", String.valueOf(user.getEnabled()));
            variables.put("accountLocked", String.valueOf(user.getAccountLocked()));
            variables.put("createdAt", user.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            variables.put("activationToken", activationToken);
            variables.put("activationLink", activationLink);
            variables.put("expirationHours", String.valueOf(expirationHours));
            variables.put("expirationDateTime", expirationDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

            // Render template
            String htmlContent = emailTemplateRenderer.renderTemplate("USER_REGISTRATION", variables);

            // Send email (this will run asynchronously)
            emailSendingService.sendHtmlEmail(
                user.getEmail(),
                "Welcome to Kabengosafaris - Activate Your Account",
                htmlContent
            );

            log.info("Registration email queued for sending to: {}", user.getEmail());

        } catch (Exception e) {
            // Log error but don't throw - email sending is async and shouldn't fail registration
            log.error("Failed to send registration email to user: {} ({})", user.getUsername(), user.getEmail(), e);
        }
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
