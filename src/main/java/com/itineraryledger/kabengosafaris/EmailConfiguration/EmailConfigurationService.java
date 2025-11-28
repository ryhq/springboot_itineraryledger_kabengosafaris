package com.itineraryledger.kabengosafaris.EmailConfiguration;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.EmailAccount.Components.EncryptionUtil;
import com.itineraryledger.kabengosafaris.Role.Role;
import com.itineraryledger.kabengosafaris.User.User;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for managing email configurations
 *
 * Responsibilities:
 * - CRUD operations for email configurations
 * - Permission management for email usage
 * - Email sending via configured accounts
 * - SMTP connection testing and validation
 * - Rate limiting and retry logic
 * - Audit logging for email operations
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class EmailConfigurationService {

    private final EmailConfigurationRepository emailConfigRepository;
    private final EmailConfigurationPermissionRepository permissionRepository;

    // Cache for JavaMailSender instances to avoid recreating them
    private final Map<Long, JavaMailSender> mailSenderCache = new HashMap<>();

    /**
     * Create a new email configuration
     * @param config the email configuration to create
     * @return the created configuration
     */
    public EmailConfiguration createConfiguration(EmailConfiguration config) {
        log.info("Creating new email configuration: {}", config.getName());

        // Validate configuration doesn't already exist
        if (emailConfigRepository.existsByName(config.getName())) {
            throw new IllegalArgumentException("Email configuration with name '" + config.getName() + "' already exists");
        }

        if (emailConfigRepository.existsByFromEmail(config.getFromEmail())) {
            throw new IllegalArgumentException("Email configuration with from email '" + config.getFromEmail() + "' already exists");
        }

        // Encrypt the SMTP password
        config.setSmtpPassword(EncryptionUtil.encrypt(config.getSmtpPassword()));

        // If this is set as default, unset all other defaults
        if (Boolean.TRUE.equals(config.getIsDefault())) {
            emailConfigRepository.setOnlyOneDefault(null); // Will be set after save
        }

        EmailConfiguration saved = emailConfigRepository.save(config);

        // Set as default if needed
        if (Boolean.TRUE.equals(config.getIsDefault())) {
            emailConfigRepository.setOnlyOneDefault(saved.getId());
        }

        // Test connection if verifyOnSave is true
        if (Boolean.TRUE.equals(config.getVerifyOnSave())) {
            testEmailConnection(saved);
        }

        log.info("Email configuration created successfully: {}", saved.getName());
        return saved;
    }

    /**
     * Update an existing email configuration
     * @param id configuration id
     * @param configUpdates the fields to update
     * @return the updated configuration
     */
    public EmailConfiguration updateConfiguration(Long id, EmailConfiguration configUpdates) {
        log.info("Updating email configuration: {}", id);

        EmailConfiguration existing = emailConfigRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Email configuration not found: " + id));

        // Update fields (but not ID, createdAt)
        if (configUpdates.getDisplayName() != null) {
            existing.setDisplayName(configUpdates.getDisplayName());
        }
        if (configUpdates.getDescription() != null) {
            existing.setDescription(configUpdates.getDescription());
        }
        if (configUpdates.getFromEmail() != null) {
            // Validate no duplicate from email
            if (!existing.getFromEmail().equals(configUpdates.getFromEmail()) &&
                emailConfigRepository.existsByFromEmail(configUpdates.getFromEmail())) {
                throw new IllegalArgumentException("From email already exists");
            }
            existing.setFromEmail(configUpdates.getFromEmail());
        }
        if (configUpdates.getFromName() != null) {
            existing.setFromName(configUpdates.getFromName());
        }
        if (configUpdates.getSmtpHost() != null) {
            existing.setSmtpHost(configUpdates.getSmtpHost());
        }
        if (configUpdates.getSmtpPort() != null) {
            existing.setSmtpPort(configUpdates.getSmtpPort());
        }
        if (configUpdates.getSmtpUsername() != null) {
            existing.setSmtpUsername(configUpdates.getSmtpUsername());
        }
        if (configUpdates.getSmtpPassword() != null) {
            // Encrypt password before storing
            existing.setSmtpPassword(EncryptionUtil.encrypt(configUpdates.getSmtpPassword()));
            // Clear mail sender cache so new one is created
            mailSenderCache.remove(id);
        }
        if (configUpdates.getUseTls() != null) {
            existing.setUseTls(configUpdates.getUseTls());
        }
        if (configUpdates.getUseSsl() != null) {
            existing.setUseSsl(configUpdates.getUseSsl());
        }
        if (configUpdates.getEnabled() != null) {
            existing.setEnabled(configUpdates.getEnabled());
        }
        if (configUpdates.getIsDefault() != null && Boolean.TRUE.equals(configUpdates.getIsDefault())) {
            existing.setIsDefault(true);
            emailConfigRepository.setOnlyOneDefault(id);
        }
        if (configUpdates.getRateLimitPerMinute() != null) {
            existing.setRateLimitPerMinute(configUpdates.getRateLimitPerMinute());
        }
        if (configUpdates.getMaxRetryAttempts() != null) {
            existing.setMaxRetryAttempts(configUpdates.getMaxRetryAttempts());
        }
        if (configUpdates.getRetryDelaySeconds() != null) {
            existing.setRetryDelaySeconds(configUpdates.getRetryDelaySeconds());
        }

        EmailConfiguration updated = emailConfigRepository.save(existing);

        // Test connection if requested
        if (Boolean.TRUE.equals(configUpdates.getVerifyOnSave())) {
            testEmailConnection(updated);
        }

        log.info("Email configuration updated: {}", id);
        return updated;
    }

    /**
     * Get configuration by ID
     */
    @Transactional(readOnly = true)
    public EmailConfiguration getConfiguration(Long id) {
        return emailConfigRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Email configuration not found: " + id));
    }

    /**
     * Get configuration by name
     */
    @Transactional(readOnly = true)
    public EmailConfiguration getConfigurationByName(String name) {
        return emailConfigRepository.findByName(name)
                .orElseThrow(() -> new IllegalArgumentException("Email configuration not found: " + name));
    }

    /**
     * Get the default email configuration
     */
    @Transactional(readOnly = true)
    public EmailConfiguration getDefaultConfiguration() {
        return emailConfigRepository.findByIsDefaultTrue()
                .orElseThrow(() -> new IllegalArgumentException("No default email configuration found"));
    }

    /**
     * Get all email configurations
     */
    @Transactional(readOnly = true)
    public List<EmailConfiguration> getAllConfigurations() {
        return emailConfigRepository.findAll();
    }

    /**
     * Get all enabled configurations
     */
    @Transactional(readOnly = true)
    public List<EmailConfiguration> getEnabledConfigurations() {
        return emailConfigRepository.findByEnabledTrue();
    }

    /**
     * Delete a configuration by ID
     */
    public void deleteConfiguration(Long id) {
        log.info("Deleting email configuration: {}", id);

        EmailConfiguration config = getConfiguration(id);

        // Cannot delete if it's the default
        if (Boolean.TRUE.equals(config.getIsDefault())) {
            throw new IllegalArgumentException("Cannot delete the default email configuration");
        }

        // Delete associated permissions
        List<EmailConfigurationPermission> permissions = permissionRepository.findByEmailConfiguration(config);
        permissionRepository.deleteAll(permissions);

        // Clear cache
        mailSenderCache.remove(id);

        emailConfigRepository.deleteById(id);
        log.info("Email configuration deleted: {}", id);
    }

    /**
     * Test SMTP connection for a configuration
     */
    public boolean testEmailConnection(EmailConfiguration config) {
        log.info("Testing email connection for: {}", config.getName());
        try {
            JavaMailSender sender = createMailSender(config);
            // Test by creating a simple message (testConnection method doesn't exist in JavaMailSender)
            SimpleMailMessage testMessage = new SimpleMailMessage();
            testMessage.setFrom(config.getFromEmail());
            testMessage.setTo(config.getFromEmail());
            testMessage.setSubject("Test Email");
            testMessage.setText("This is a test email");
            sender.send(testMessage);

            config.setLastTestedAt(LocalDateTime.now());
            config.setLastErrorMessage(null);
            emailConfigRepository.save(config);

            log.info("Email connection test successful for: {}", config.getName());
            return true;
        } catch (Exception e) {
            log.error("Email connection test failed for: {}", config.getName(), e);
            config.setLastErrorMessage(e.getMessage());
            emailConfigRepository.save(config);
            return false;
        }
    }

    /**
     * Create a JavaMailSender for a configuration
     * Caches senders to avoid recreating them
     */
    private JavaMailSender createMailSender(EmailConfiguration config) {
        return mailSenderCache.computeIfAbsent(config.getId(), key -> {
            JavaMailSenderImpl sender = new JavaMailSenderImpl();
            sender.setHost(config.getSmtpHost());
            sender.setPort(config.getSmtpPort());
            sender.setUsername(config.getSmtpUsername());
            sender.setPassword(EncryptionUtil.decrypt(config.getSmtpPassword()));

            Properties props = sender.getJavaMailProperties();
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enabled", config.getUseTls());
            props.put("mail.smtp.starttls.required", config.getUseTls());
            props.put("mail.smtp.ssl.enable", config.getUseSsl());
            props.put("mail.smtp.socketFactory.protocol", config.getUseSsl() ? "SSLv23" : "tcp");

            return sender;
        });
    }

    /**
     * Get a JavaMailSender for a configuration
     */
    public JavaMailSender getMailSender(Long configId) {
        EmailConfiguration config = getConfiguration(configId);
        if (!config.getEnabled()) {
            throw new IllegalArgumentException("Configuration is disabled: " + configId);
        }
        return createMailSender(config);
    }

    // ======================== PERMISSION MANAGEMENT ========================

    /**
     * Grant a user access to use an email configuration
     */
    public EmailConfigurationPermission grantUserAccess(Long configId, Long userId, String grantReason, String grantedBy) {
        return grantUserAccess(configId, userId, grantReason, grantedBy, null, 0);
    }

    /**
     * Grant a user access to use an email configuration with optional expiration
     */
    public EmailConfigurationPermission grantUserAccess(
            Long configId, Long userId, String grantReason, String grantedBy,
            LocalDateTime expiresAt, Integer maxEmailsPerDay) {

        EmailConfiguration config = getConfiguration(configId);

        User user = User.builder().id(userId).build();

        EmailConfigurationPermission permission = EmailConfigurationPermission.builder()
                .emailConfiguration(config)
                .user(user)
                .grantReason(grantReason)
                .grantedBy(grantedBy)
                .expiresAt(expiresAt)
                .maxEmailsPerDay(maxEmailsPerDay)
                .enabled(true)
                .build();

        EmailConfigurationPermission saved = permissionRepository.save(permission);
        log.info("Granted user {} access to config {}", userId, configId);
        return saved;
    }

    /**
     * Grant a role access to use an email configuration
     */
    public EmailConfigurationPermission grantRoleAccess(Long configId, Long roleId, String grantReason, String grantedBy) {
        return grantRoleAccess(configId, roleId, grantReason, grantedBy, null, 0);
    }

    /**
     * Grant a role access to use an email configuration with optional expiration
     */
    public EmailConfigurationPermission grantRoleAccess(
            Long configId, Long roleId, String grantReason, String grantedBy,
            LocalDateTime expiresAt, Integer maxEmailsPerDay) {

        EmailConfiguration config = getConfiguration(configId);

        Role role = Role.builder().id(roleId).build();

        EmailConfigurationPermission permission = EmailConfigurationPermission.builder()
                .emailConfiguration(config)
                .role(role)
                .grantReason(grantReason)
                .grantedBy(grantedBy)
                .expiresAt(expiresAt)
                .maxEmailsPerDay(maxEmailsPerDay)
                .enabled(true)
                .build();

        EmailConfigurationPermission saved = permissionRepository.save(permission);
        log.info("Granted role {} access to config {}", roleId, configId);
        return saved;
    }

    /**
     * Check if a user has access to a configuration
     */
    @Transactional(readOnly = true)
    public boolean userHasAccessToConfig(Long configId, Long userId) {
        return permissionRepository.userHasAccessToConfig(configId, userId, LocalDateTime.now());
    }

    /**
     * Check if a role has access to a configuration
     */
    @Transactional(readOnly = true)
    public boolean roleHasAccessToConfig(Long configId, Long roleId) {
        return permissionRepository.roleHasAccessToConfig(configId, roleId, LocalDateTime.now());
    }

    /**
     * Check if a user or any of their roles have access to a configuration
     */
    @Transactional(readOnly = true)
    public boolean userOrRoleHasAccessToConfig(Long configId, User user) {
        if (userHasAccessToConfig(configId, user.getId())) {
            return true;
        }

        return user.getRoles().stream()
                .anyMatch(role -> roleHasAccessToConfig(configId, role.getId()));
    }

    /**
     * Get all configurations a user has access to
     */
    @Transactional(readOnly = true)
    public List<EmailConfiguration> getAccessibleConfigurationsForUser(Long userId) {
        List<EmailConfigurationPermission> userPerms = permissionRepository.findByUserIdAndEnabledTrue(userId);

        return userPerms.stream()
                .filter(EmailConfigurationPermission::isValid)
                .map(EmailConfigurationPermission::getEmailConfiguration)
                .filter(EmailConfiguration::getEnabled)
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * Get all configurations a role has access to
     */
    @Transactional(readOnly = true)
    public List<EmailConfiguration> getAccessibleConfigurationsForRole(Long roleId) {
        List<EmailConfigurationPermission> rolePerms = permissionRepository.findByRoleIdAndEnabledTrue(roleId);

        return rolePerms.stream()
                .filter(EmailConfigurationPermission::isValid)
                .map(EmailConfigurationPermission::getEmailConfiguration)
                .filter(EmailConfiguration::getEnabled)
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * Revoke user access to a configuration
     */
    public void revokeUserAccess(Long configId, Long userId) {
        List<EmailConfigurationPermission> perms = permissionRepository
                .findByEmailConfigurationAndUserId(getConfiguration(configId), userId);

        permissionRepository.deleteAll(perms);
        log.info("Revoked user {} access to config {}", userId, configId);
    }

    /**
     * Disable expired permissions
     */
    public void disableExpiredPermissions() {
        List<EmailConfigurationPermission> expired = permissionRepository.findExpiredPermissions(LocalDateTime.now());
        expired.forEach(perm -> perm.setEnabled(false));
        permissionRepository.saveAll(expired);
        log.info("Disabled {} expired email configuration permissions", expired.size());
    }

    /**
     * Record successful email send
     */
    public void recordSuccessfulSend(Long configId) {
        emailConfigRepository.incrementEmailsSentCount(configId);
        log.debug("Recorded successful email send for config: {}", configId);
    }

    /**
     * Record failed email send
     */
    public void recordFailedSend(Long configId) {
        emailConfigRepository.incrementEmailsFailedCount(configId);
        log.debug("Recorded failed email send for config: {}", configId);
    }
}
