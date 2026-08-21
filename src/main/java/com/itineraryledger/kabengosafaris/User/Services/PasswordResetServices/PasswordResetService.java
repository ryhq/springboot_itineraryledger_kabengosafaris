package com.itineraryledger.kabengosafaris.User.Services.PasswordResetServices;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.itineraryledger.kabengosafaris.EmailAccount.EmailAccountServices.EmailSendingService;
import com.itineraryledger.kabengosafaris.EmailEvent.Services.EmailTemplateRenderer;
import com.itineraryledger.kabengosafaris.Security.JwtTokenProvider;
import com.itineraryledger.kabengosafaris.Security.PasswordHasher;
import com.itineraryledger.kabengosafaris.Security.PasswordValidator;
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
 * Service for handling password reset functionality.
 *
 * Provides methods to:
 * - Request password reset email (forgot password)
 * - Reset password using token
 */
@Service
@Slf4j
public class PasswordResetService {

    @org.springframework.beans.factory.annotation.Autowired
    private com.itineraryledger.kabengosafaris.CompanyProfile.Services.CompanyIdentityService companyIdentityService;

    @org.springframework.beans.factory.annotation.Value("${app.company.name:}")
    private String configuredCompanyName;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private PasswordValidator passwordValidator;

    @Autowired
    private EmailTemplateRenderer emailTemplateRenderer;

    @Autowired
    private EmailSendingService emailSendingService;

    @Autowired
    private SecuritySettingsGetterServices securitySettingsGetterServices;

    @Value("${app.management.base.url}")
    private String appManagementBaseUrl;

    /**
     * Request a password reset email.
     *
     * This method is designed to prevent email enumeration attacks by always
     * returning successfully, regardless of whether the email exists.
     *
     * @param email The user's email address
     * @throws PasswordResetException only if email parameter is missing
     */
    public void requestPasswordReset(String email) {
        log.info("Processing password reset request for email: {}", email);

        if (email == null || email.isBlank()) {
            throw new PasswordResetException("Email is required");
        }

        String normalizedEmail = email.toLowerCase().trim();
        User user = userRepository.findByEmail(normalizedEmail).orElse(null);

        // Prevent email enumeration - don't reveal if email exists
        if (user == null) {
            log.info("Password reset requested for non-existent email: {}", normalizedEmail);
            return;
        }

        // Don't send reset email if account is not enabled
        if (!user.getEnabled()) {
            log.info("Password reset requested for non-enabled account: {}", normalizedEmail);
            return;
        }

        // Don't send reset email if account is locked
        if (user.getAccountLocked()) {
            log.info("Password reset requested for locked account: {}", normalizedEmail);
            return;
        }

        // Send the password reset email
        sendPasswordResetEmail(user);
        log.info("Password reset email sent to: {}", normalizedEmail);
    }

    /**
     * Reset user password using the reset token.
     *
     * @param token The password reset token
     * @param newPassword The new password
     * @throws PasswordResetException if token is invalid, expired, or password validation fails
     */
    public void resetPassword(String token, String newPassword) {
        log.info("Processing password reset with token");

        // Validate token is not empty
        if (token == null || token.isBlank()) {
            throw new PasswordResetException("Reset token is required");
        }

        // Validate new password is not empty
        if (newPassword == null || newPassword.isBlank()) {
            throw new PasswordResetException("New password is required");
        }

        // Validate token signature and expiration
        if (!jwtTokenProvider.validateToken(token)) {
            throw new PasswordResetException("Invalid or expired reset token");
        }

        // Verify token type is PASSWORD_RESET
        TokenType tokenType = jwtTokenProvider.getTokenType(token);
        if (tokenType != TokenType.PASSWORD_RESET) {
            log.warn("Invalid token type for password reset: {}", tokenType);
            throw new PasswordResetException("Invalid token type for password reset");
        }

        // Extract username from token
        String username = jwtTokenProvider.getUsernameFromToken(token);
        if (username == null || username.isBlank()) {
            throw new PasswordResetException("Invalid reset token");
        }

        // Find user by username
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) {
            log.warn("User not found for password reset: {}", username);
            throw new PasswordResetException("User not found");
        }

        /*
         * Is this link still live?
         *
         * A reset token is a stateless JWT — signature and expiry are all it can be judged on by
         * itself — so the same link worked repeatedly for its whole lifetime, including long after
         * its owner had finished resetting. Anybody who came across it later (a shared inbox, a
         * forwarded message, browser history, a proxy log) could set the password again.
         *
         * A link minted before the current password was set is spent. That also kills an older link
         * the moment a newer one is used, which is the same fault wearing a different hat.
         *
         * The message deliberately does not distinguish "already used" from "expired": both mean ask
         * for a new one, and the difference tells a stranger holding the link whether they were
         * first.
         */
        java.time.LocalDateTime issuedAt = jwtTokenProvider.getIssuedAt(token);
        if (user.getPasswordChangedAt() != null
                && (issuedAt == null || !issuedAt.isAfter(user.getPasswordChangedAt()))) {
            log.warn("Rejected a spent password-reset link for user: {}", username);
            throw new PasswordResetException("Invalid or expired reset token");
        }

        // Validate password policy
        try {
            passwordValidator.validatePassword(newPassword);
        } catch (IllegalArgumentException e) {
            throw new PasswordResetException(e.getMessage());
        }

        // Hash and save the new password
        String hashedPassword = PasswordHasher.hashPassword(newPassword);
        user.setPassword(hashedPassword);
        /* stamped BEFORE the save, so this link and every older one are spent from here on */
        user.setPasswordChangedAt(java.time.LocalDateTime.now());

        // Reset password expiry date if configured
        try {
            int expirationDays = securitySettingsGetterServices.getPasswordExpirationDays();
            if (expirationDays > 0) {
                user.setPasswordExpiryDate(LocalDateTime.now().plusDays(expirationDays));
            }
        } catch (Exception e) {
            log.warn("Could not set password expiry date: {}", e.getMessage());
        }

        // Reset failed login attempts if any
        user.setFailedAttempt(0);
        user.setLastFailedAttemptTime(null);

        userRepository.save(user);
        log.info("Password reset successfully for user: {}", username);
    }

    /**
     * Send password reset email with token and link.
     *
     * @param user The user to send the password reset email to
     */
    private void sendPasswordResetEmail(User user) {
        try {
            log.info("Preparing to send password reset email to user: {} ({})", user.getUsername(), user.getEmail());

            // Generate password reset token
            String resetToken = jwtTokenProvider.generatePasswordResetTokenFromUsername(user.getUsername());

            // Build reset link
            String resetLink = appManagementBaseUrl + "/reset-password?token=" + resetToken;

            // Calculate expiration time
            Long expirationMinutes = securitySettingsGetterServices.getPasswordResetJwtExpirationMinutes();
            LocalDateTime expirationDateTime = LocalDateTime.now().plusMinutes(expirationMinutes);

            log.debug("Reset link generated for user {}: {} (expires in {} minutes)",
                user.getUsername(), resetLink, expirationMinutes);

            // Prepare template variables
            Map<String, String> variables = new HashMap<>();
            variables.put("username", user.getUsername());
            variables.put("email", user.getEmail());
            variables.put("firstName", user.getFirstName() != null ? user.getFirstName() : "");
            variables.put("lastName", user.getLastName() != null ? user.getLastName() : "");
            variables.put("resetToken", resetToken);
            variables.put("resetLink", resetLink);
            variables.put("expirationMinutes", String.valueOf(expirationMinutes));
            variables.put("expirationDateTime", expirationDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            variables.put("requestedAt", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

            // Render template
            String htmlContent = emailTemplateRenderer.renderTemplate("PASSWORD_RESET", variables);

            // Send email
            emailSendingService.sendHtmlEmail(
                user.getEmail(),
                "Password Reset Request - " + companyName(),
                htmlContent
            );

            log.info("Password reset email queued for sending to: {}", user.getEmail());

        } catch (Exception e) {
            log.error("Failed to send password reset email to user: {} ({})", user.getUsername(), user.getEmail(), e);
            // Don't throw - we don't want to reveal if email sending failed
        }
    }

    /** Whose name goes on this message — a literal here welcomed one company's users to another. */
    private String companyName() {
        try {
            String name = companyIdentityService.snapshot().name();
            if (name != null && !name.isBlank()) return name;
        } catch (Exception ignored) {
            /* a message with a plain subject beats no message at all */
        }
        return configuredCompanyName == null || configuredCompanyName.isBlank()
            ? "your account" : configuredCompanyName;
    }
}
