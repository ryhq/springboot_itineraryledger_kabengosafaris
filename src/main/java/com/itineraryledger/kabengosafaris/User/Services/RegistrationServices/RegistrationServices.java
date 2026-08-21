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

    @org.springframework.beans.factory.annotation.Autowired
    private com.itineraryledger.kabengosafaris.CompanyProfile.Services.CompanyIdentityService companyIdentityService;

    @org.springframework.beans.factory.annotation.Value("${app.company.name:}")
    private String configuredCompanyName;

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

    @Value("${app.management.base.url}")
    private String appManagementBaseUrl;

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
        return registerUser(request, true);
    }

    /**
     * Register a new user, choosing whether to email the activation link.
     *
     * An administrator creating an account for a colleague goes through exactly this
     * path — same uniqueness checks, same password policy, same expiry calculation —
     * but occasionally wants the account in place without a mail going out yet (the
     * person starts next month, or the invite will be resent later). Only that one
     * decision differs, so it is a parameter rather than a second copy of the method.
     *
     * @param sendActivationEmail false to create the account silently; it stays
     *                            disabled either way until somebody activates it
     */
    /**
     * A registration nobody has completed: never activated, and holding no roles.
     *
     * Both halves matter. `enabled == false` alone would also describe an account an administrator
     * deliberately deactivated, and handing that address to the next person who asks for it would be
     * a way to take over somebody's account by filling in a form.
     */
    private boolean isUnclaimed(User user) {
        return user != null
            && !Boolean.TRUE.equals(user.getEnabled())
            && (user.getRoles() == null || user.getRoles().isEmpty());
    }

    private boolean sameRow(User a, User b) {
        return a != null && b != null && a.getId() != null && a.getId().equals(b.getId());
    }

    public User registerUser(RegistrationRequest request, boolean sendActivationEmail) {
        // Validate all required fields
        validateRegistrationRequest(request);

        /*
         * An address nobody has proved they control does not get to own itself forever.
         *
         * The row was written before the activation email went out, and a second attempt was refused
         * outright — so anyone could type a colleague's address, or the company's own info@, and that
         * address was permanently unusable. Nothing had to be compromised: filling in a form was
         * enough, and the person locked out had no way to tell why.
         *
         * A pending registration — never activated, no roles, so nobody has ever signed in as it —
         * is a claim, not an account. A fresh attempt on the same address replaces it and re-sends
         * the activation. Whoever actually reads that mailbox is the one who ends up with the
         * account, which is the only test that means anything here.
         *
         * An ACTIVATED account still refuses, because it belongs to somebody.
         */
        User pending = userRepository.findByEmail(request.getEmail())
            .filter(this::isUnclaimed)
            .orElse(null);

        if (pending == null && userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RegistrationException("Email already registered");
        }

        User usernameHolder = userRepository.findByUsername(request.getUsername()).orElse(null);
        if (usernameHolder != null && !sameRow(usernameHolder, pending)) {
            /* a pending row of its own is replaceable for the same reason as the address */
            if (isUnclaimed(usernameHolder) && pending == null) {
                pending = usernameHolder;
            } else {
                throw new RegistrationException("Username already exists");
            }
        }

        if (request.getPhoneNumber() != null) {
            User phoneHolder = userRepository.findByPhoneNumber(request.getPhoneNumber()).orElse(null);
            if (phoneHolder != null && !sameRow(phoneHolder, pending)) {
                if (isUnclaimed(phoneHolder) && pending == null) {
                    pending = phoneHolder;
                } else {
                    throw new RegistrationException("Phone already exists");
                }
            }
        }

        if (pending != null) {
            log.info("Replacing an unactivated registration for {} rather than refusing the address",
                request.getEmail());
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
        User user = pending != null ? pending : User.builder()
                .email(request.getEmail().toLowerCase().trim())
                .username(request.getUsername().trim())
                .password(hashedPassword)
                .firstName(request.getFirstName().trim())
                .lastName(request.getLastName().trim())
                .phoneNumber(request.getPhoneNumber() != null ? request.getPhoneNumber().trim() : null)
                .enabled(false) // Disabled until email verification
                /* the first password is a password change too, so reset links are judged against it */
                .passwordChangedAt(LocalDateTime.now())
                .accountLocked(false)
                .failedAttempt(0)
                .build();

        /*
         * Overwrite the claim in place when there was one, so its id, and anything already pointing
         * at it, survive — and so the unique constraints on email/username/phone are not fought over
         * by two rows for the same person.
         */
        if (pending != null) {
            user.setEmail(request.getEmail().toLowerCase().trim());
            user.setUsername(request.getUsername().trim());
            user.setPassword(hashedPassword);
            user.setFirstName(request.getFirstName().trim());
            user.setLastName(request.getLastName().trim());
            user.setPhoneNumber(request.getPhoneNumber() != null ? request.getPhoneNumber().trim() : null);
            user.setEnabled(false);
            user.setAccountLocked(false);
            user.setFailedAttempt(0);
            user.setPasswordChangedAt(LocalDateTime.now());
        }

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
        if (sendActivationEmail) {
            sendRegistrationEmail(savedUser);
        } else {
            log.info("Account {} created without an activation email, as requested", savedUser.getUsername());
        }

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
                "Welcome to " + companyName() + " - Activate Your Account",
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


    /**
     * Whose name goes on this message.
     *
     * The subject used to be a literal, so a second company's users were welcomed to the first
     * company. The profile is the source; the configured name is the fallback for the moment before
     * anybody has filled the profile in.
     */
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
