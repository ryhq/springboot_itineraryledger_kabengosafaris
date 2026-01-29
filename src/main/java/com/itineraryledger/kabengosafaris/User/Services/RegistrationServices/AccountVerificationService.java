package com.itineraryledger.kabengosafaris.User.Services.RegistrationServices;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.itineraryledger.kabengosafaris.EmailAccount.EmailAccountServices.EmailSendingService;
import com.itineraryledger.kabengosafaris.EmailEvent.Services.EmailTemplateRenderer;
import com.itineraryledger.kabengosafaris.Security.JwtTokenProvider;
import com.itineraryledger.kabengosafaris.Security.TokenType;
import com.itineraryledger.kabengosafaris.Security.SecuritySettings.SecuritySettingsGetterServices;
import com.itineraryledger.kabengosafaris.User.User;
import com.itineraryledger.kabengosafaris.User.UserRepository;

import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for handling user account verification after registration.
 *
 * Provides methods to:
 * - Verify user account using activation token
 * - Resend verification email
 */
@Service
@Slf4j
public class AccountVerificationService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private EmailTemplateRenderer emailTemplateRenderer;

    @Autowired
    private EmailSendingService emailSendingService;

    @Autowired
    private SecuritySettingsGetterServices securitySettingsGetterServices;

    @Value("${app.management.base.url}")
    private String appManagementBaseUrl;

    /**
     * Verify user account using activation token.
     * This method validates the token and enables the user account.
     *
     * @param token The activation token from the registration email
     * @return The verified and enabled User entity
     * @throws RegistrationException if token is invalid, expired, or user not found
     */
    public User accountActivation(String token) {
        log.info("Attempting to verify account with token");

        // Validate token is not empty
        if (token == null || token.isBlank()) {
            throw new RegistrationException("Activation token is required");
        }

        // Validate token signature and expiration
        if (!jwtTokenProvider.validateToken(token)) {
            throw new RegistrationException("Invalid or expired activation token");
        }

        // Verify token type is REGISTRATION
        TokenType tokenType = jwtTokenProvider.getTokenType(token);
        if (tokenType != TokenType.REGISTRATION) {
            log.warn("Invalid token type for account verification: {}", tokenType);
            throw new RegistrationException("Invalid token type for account verification");
        }

        // Extract username from token
        String username = jwtTokenProvider.getUsernameFromToken(token);
        if (username == null || username.isBlank()) {
            throw new RegistrationException("Invalid activation token");
        }

        // Find user by username
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) {
            log.warn("User not found for activation: {}", username);
            throw new RegistrationException("User not found");
        }

        // Check if already verified
        if (user.getEnabled()) {
            log.info("User {} is already verified", username);
            throw new RegistrationException("Account is already verified");
        }

        // Enable the user account
        user.setEnabled(true);
        User savedUser = userRepository.save(user);

        log.info("Successfully verified and enabled account for user: {}", username);
        return savedUser;
    }

    /**
     * Resend verification email to a user who hasn't verified their account yet.
     *
     * This method is designed to prevent email enumeration attacks by always
     * returning successfully, regardless of whether the email exists or the
     * account is already verified. The actual outcome is only logged internally.
     *
     * @param email The user's email address
     * @throws RegistrationException only if email parameter is missing
     */
    public void resendVerificationEmail(String email) {
        log.info("Processing resend verification request for email: {}", email);

        if (email == null || email.isBlank()) {
            throw new RegistrationException("Email is required");
        }

        String normalizedEmail = email.toLowerCase().trim();
        User user = userRepository.findByEmail(normalizedEmail).orElse(null);

        // Prevent email enumeration - don't reveal if email exists or account status
        if (user == null) {
            log.info("Resend verification requested for non-existent email: {}", normalizedEmail);
            // Return silently without revealing that the email doesn't exist
            return;
        }

        if (user.getEnabled()) {
            log.info("Resend verification requested for already verified account: {}", normalizedEmail);
            // Return silently without revealing that the account is already verified
            return;
        }

        if (user.getAccountLocked()) {
            log.info("Resend verification requested for locked account: {}", normalizedEmail);
            // Return silently without revealing that the account is locked
            return;
        }

        // Only send the email if user exists and is not yet verified
        sendVerificationEmail(user);
        log.info("Verification email resent to: {}", normalizedEmail);
    }

    /**
     * Send verification email with activation token and link.
     *
     * @param user The user to send the verification email to
     */
    private void sendVerificationEmail(User user) {
        try {
            log.info("Preparing to send verification email to user: {} ({})", user.getUsername(), user.getEmail());

            // Generate activation token
            String activationToken = jwtTokenProvider.generateRegistrationTokenFromUsername(user.getUsername());

            // Build activation link
            String activationLink = appManagementBaseUrl + "/account-activation?token=" + activationToken;

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
            variables.put("createdAt", user.getCreatedAt() != null
                ? user.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                : LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            variables.put("activationToken", activationToken);
            variables.put("activationLink", activationLink);
            variables.put("expirationHours", String.valueOf(expirationHours));
            variables.put("expirationDateTime", expirationDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

            // Render template
            String htmlContent = emailTemplateRenderer.renderTemplate("USER_REGISTRATION", variables);

            // Send email (this will run asynchronously)
            emailSendingService.sendHtmlEmail(
                user.getEmail(),
                "Activate Your Account - Kabengosafaris",
                htmlContent
            );

            log.info("Verification email queued for sending to: {}", user.getEmail());

        } catch (Exception e) {
            log.error("Failed to send verification email to user: {} ({})", user.getUsername(), user.getEmail(), e);
            throw new RegistrationException("Failed to send verification email");
        }
    }
}
