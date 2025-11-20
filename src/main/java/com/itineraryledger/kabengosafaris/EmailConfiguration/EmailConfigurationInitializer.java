package com.itineraryledger.kabengosafaris.EmailConfiguration;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * EmailConfigurationInitializer - Initializes default email configurations on startup
 *
 * This component:
 * - Creates default email configurations if they don't exist
 * - Sets up placeholder configurations for common email providers
 * - Ensures at least one default configuration exists
 * - Runs after other initializers (AuditLogConfigInitializer, SecuritySettingsInitializer)
 *
 * Default configurations created:
 * - Default SMTP: Generic configuration for custom SMTP servers
 * - Gmail: Pre-configured for Gmail SMTP
 * - Outlook: Pre-configured for Outlook SMTP
 *
 * NOTE: These are PLACEHOLDER configurations - they must be updated with actual credentials
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Order(50) // Run after other initializers (SecuritySettingsInitializer=10, AuditLogConfigInitializer=20)
public class EmailConfigurationInitializer {

    private final EmailConfigurationRepository emailConfigRepository;

    @EventListener(ApplicationReadyEvent.class)
    public void initializeEmailConfigurations() {
        log.info("Initializing email configurations...");

        try {
            // Create Gmail configuration template if it doesn't exist
            if (!emailConfigRepository.existsByName("gmail")) {
                createGmailConfiguration();
            }

            // Create Outlook configuration template if it doesn't exist
            if (!emailConfigRepository.existsByName("outlook")) {
                createOutlookConfiguration();
            }

            // Create generic SMTP configuration if it doesn't exist
            if (!emailConfigRepository.existsByName("default")) {
                createDefaultSmtpConfiguration();
            }

            // Ensure at least one default configuration exists
            if (emailConfigRepository.findByIsDefaultTrue().isEmpty()) {
                setDefaultConfiguration();
            }

            log.info("Email configurations initialized successfully");

        } catch (Exception e) {
            log.error("Error initializing email configurations", e);
        }
    }

    /**
     * Create a Gmail SMTP configuration template
     * Users must update with their actual Gmail credentials
     */
    private void createGmailConfiguration() {
        log.info("Creating Gmail email configuration template");

        EmailConfiguration gmail = EmailConfiguration.builder()
                .name("gmail")
                .displayName("Gmail SMTP")
                .description("Gmail SMTP configuration - Update with your credentials")
                .fromEmail("your-email@gmail.com")
                .fromName("Kabengo Safaris")
                .smtpHost("smtp.gmail.com")
                .smtpPort(587)
                .smtpUsername("your-email@gmail.com")
                // Placeholder password - MUST BE UPDATED
                .smtpPassword(EncryptionUtil.encrypt("your-app-password"))
                .useTls(true)
                .useSsl(false)
                .providerType("GMAIL")
                .enabled(false) // Disabled by default until configured
                .isDefault(false)
                .rateLimitPerMinute(0)
                .maxRetryAttempts(3)
                .retryDelaySeconds(5)
                .verifyOnSave(false)
                .emailsSentCount(0L)
                .emailsFailedCount(0L)
                .createdBy("SYSTEM")
                .updatedBy("SYSTEM")
                .build();

        emailConfigRepository.save(gmail);
        log.info("Gmail configuration template created");
    }

    /**
     * Create an Outlook SMTP configuration template
     * Users must update with their actual Outlook credentials
     */
    private void createOutlookConfiguration() {
        log.info("Creating Outlook email configuration template");

        EmailConfiguration outlook = EmailConfiguration.builder()
                .name("outlook")
                .displayName("Outlook/Office365 SMTP")
                .description("Outlook/Office365 SMTP configuration - Update with your credentials")
                .fromEmail("your-email@outlook.com")
                .fromName("Kabengo Safaris")
                .smtpHost("smtp.office365.com")
                .smtpPort(587)
                .smtpUsername("your-email@outlook.com")
                // Placeholder password - MUST BE UPDATED
                .smtpPassword(EncryptionUtil.encrypt("your-password"))
                .useTls(true)
                .useSsl(false)
                .providerType("OUTLOOK")
                .enabled(false) // Disabled by default until configured
                .isDefault(false)
                .rateLimitPerMinute(0)
                .maxRetryAttempts(3)
                .retryDelaySeconds(5)
                .verifyOnSave(false)
                .emailsSentCount(0L)
                .emailsFailedCount(0L)
                .createdBy("SYSTEM")
                .updatedBy("SYSTEM")
                .build();

        emailConfigRepository.save(outlook);
        log.info("Outlook configuration template created");
    }

    /**
     * Create a generic SMTP configuration template
     * Users can customize this for any SMTP server
     */
    private void createDefaultSmtpConfiguration() {
        log.info("Creating default SMTP email configuration template");

        EmailConfiguration defaultConfig = EmailConfiguration.builder()
                .name("default")
                .displayName("Default SMTP Configuration")
                .description("Generic SMTP configuration - Customize for your mail server")
                .fromEmail("noreply@kabengosafaris.com")
                .fromName("Kabengo Safaris")
                .smtpHost("smtp.example.com")
                .smtpPort(587)
                .smtpUsername("your-email@example.com")
                // Placeholder password - MUST BE UPDATED
                .smtpPassword(EncryptionUtil.encrypt("your-password"))
                .useTls(true)
                .useSsl(false)
                .providerType("CUSTOM")
                .enabled(false) // Disabled by default until configured
                .isDefault(true) // Set as default
                .rateLimitPerMinute(0)
                .maxRetryAttempts(3)
                .retryDelaySeconds(5)
                .verifyOnSave(false)
                .emailsSentCount(0L)
                .emailsFailedCount(0L)
                .createdBy("SYSTEM")
                .updatedBy("SYSTEM")
                .build();

        emailConfigRepository.save(defaultConfig);
        log.info("Default SMTP configuration template created");
    }

    /**
     * Ensure at least one configuration is marked as default
     * If none are marked as default, set the first one
     */
    private void setDefaultConfiguration() {
        log.info("Setting default email configuration");

        EmailConfiguration first = emailConfigRepository.findAll().stream()
                .findFirst()
                .orElse(null);

        if (first != null) {
            first.setIsDefault(true);
            emailConfigRepository.save(first);
            log.info("Set '{}' as default email configuration", first.getName());
        }
    }
}
