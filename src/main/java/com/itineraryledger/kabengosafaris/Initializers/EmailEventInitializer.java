package com.itineraryledger.kabengosafaris.Initializers;

import com.itineraryledger.kabengosafaris.EmailEvent.EmailEventRepository;
import com.itineraryledger.kabengosafaris.EmailEvent.EmailEventVariables;
import com.itineraryledger.kabengosafaris.EmailEvent.ModalEntity.EmailEvent;
import com.itineraryledger.kabengosafaris.EmailEvent.Services.EmailTemplateCreateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Initializer for Email Events and their System Default Templates.
 * Runs at application startup and initializes predefined email events in the database.
 *
 * This ensures that the system has the required email events for various notification scenarios.
 * Each event is created with system-defined variables and a system default template.
 *
 * Email Events:
 * - USER_REGISTRATION: Sent when a new user registers
 * - PASSWORD_RESET: Sent when a user requests to reset their password
 * - BACKUP_SUCCESS: Sent when a backup completes successfully
 * - BACKUP_FAILURE: Sent when a backup fails
 *
 * Note: Other events (EMAIL_VERIFICATION, ACCOUNT_ACTIVATED, etc.) will be added in future iterations.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EmailEventInitializer implements ApplicationRunner, Ordered {

    private final EmailEventRepository emailEventRepository;
    private final EmailTemplateCreateService emailTemplateCreateService;

    /**
     * Run initialization at application startup
     * Priority: Run fifth after RoleInitializer
     */
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 5;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        printStartBanner();
        boolean success = false;

        try {
            initializeEmailEvents();
            success = true;
        } catch (Exception e) {
            log.error("Error during email event initialization: {}", e.getMessage(), e);
            success = false;
        } finally {
            printEndBanner(success);
        }
    }

    /**
     * Print start banner for email event initialization
     */
    private void printStartBanner() {
        log.info("");
        log.info("╔════════════════════════════════════════════════════════════════════╗");
        log.info("║                                                                    ║");
        log.info("║              EMAIL EVENT INITIALIZER - START                       ║");
        log.info("║                                                                    ║");
        log.info("╚════════════════════════════════════════════════════════════════════╝");
        log.info("");
    }

    /**
     * Print end banner for email event initialization
     *
     * @param success whether the initialization was successful
     */
    private void printEndBanner(boolean success) {
        log.info("");
        if (success) {
            log.info("╔════════════════════════════════════════════════════════════════════╗");
            log.info("║                                                                    ║");
            log.info("║          ✓ EMAIL EVENT INITIALIZER - COMPLETED                     ║");
            log.info("║                                                                    ║");
            log.info("╚════════════════════════════════════════════════════════════════════╝");
        } else {
            log.info("╔════════════════════════════════════════════════════════════════════╗");
            log.info("║                                                                    ║");
            log.info("║          ✗ EMAIL EVENT INITIALIZER - FAILED                        ║");
            log.info("║                                                                    ║");
            log.info("╚════════════════════════════════════════════════════════════════════╝");
        }
        log.info("");
    }

    /**
     * Initialize predefined email events
     */
    private void initializeEmailEvents() {
        // ========================================
        // User Management Events
        // ========================================

        initializeEvent(
            "USER_REGISTRATION",
            "Sent when a new user registers in the system. Contains welcome message and account activation instructions."
        );

        initializeEvent(
            "PASSWORD_RESET",
            "Sent when a user requests to reset their password. Contains password reset link and instructions."
        );

        // ========================================
        // Backup Events
        // ========================================

        initializeEvent(
            "BACKUP_SUCCESS",
            "Sent when a scheduled or manual backup completes successfully. Contains backup details including size, location, and next scheduled backup time."
        );

        initializeEvent(
            "BACKUP_FAILURE",
            "Sent when a scheduled or manual backup fails. Contains error details, troubleshooting steps, and next backup attempt time. Requires immediate attention."
        );

        // ========================================
        // Public Website Events
        // ========================================

        initializeEvent(
            "NEWSLETTER_SUBSCRIPTION",
            "Sent when someone subscribes to the newsletter. Contains subscriber details and subscription source."
        );

        initializeEvent(
            "BOOKING_INQUIRY",
            "Sent when a new booking inquiry is submitted from the website. Contains full traveler details, travel preferences, and itinerary information."
        );

        initializeEvent(
            "CONTACT_US",
            "Sent when someone submits a message through the website Contact Us form. Contains sender details, subject, and message content."
        );

        // TODO: Add other events in future iterations:
        // - EMAIL_VERIFICATION
        // - ACCOUNT_ACTIVATED
        // - ACCOUNT_DEACTIVATED
        // - PASSWORD_CHANGED
    }

    /**
     * Initialize a single email event with system-defined variables and default template
     */
    private void initializeEvent(String eventName, String description) {
        try {
            // Check if event already exists
            if (emailEventRepository.existsByName(eventName)) {
                log.debug("⊘ Email event already exists: {}", eventName);
                return;
            }

            // Get system-defined variables for this event
            String variablesJson = EmailEventVariables.getVariablesForEvent(eventName);

            // Create email event with variables
            EmailEvent event = EmailEvent.builder()
                .name(eventName)
                .description(description)
                .enabled(true)
                .variablesJson(variablesJson)
                .build();

            EmailEvent savedEvent = emailEventRepository.save(event);
            log.info("Created email event: {} with {} system variables",
                eventName, countVariables(variablesJson));

            // Create system default template for this event
            boolean templateCreated = emailTemplateCreateService.createSystemDefaultTemplate(savedEvent);
            if (templateCreated) {
                log.info("Created system default template for event: {}", eventName);
            } else {
                log.warn("Failed to create system default template for event: {}", eventName);
            }

        } catch (Exception e) {
            log.error("Failed to initialize email event: {}", eventName, e);
        }
    }

    /**
     * Count the number of variables in the JSON array
     */
    private int countVariables(String variablesJson) {
        try {
            return variablesJson.split("\\{").length - 1;
        } catch (Exception e) {
            return 0;
        }
    }
}
