package com.itineraryledger.kabengosafaris.EmailEvent;

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
 * - USER_REGISTRATION: Sent when a new user registers (currently implemented)
 *
 * Note: Other events (PASSWORD_RESET, EMAIL_VERIFICATION, etc.) will be added in future iterations.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EmailEventInitializer implements ApplicationRunner, Ordered {

    private final EmailEventRepository emailEventRepository;
    private final EmailTemplateCreateService emailTemplateCreateService;

    /**
     * Run initialization at application startup
     * Priority: After security settings but before other components
     */
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 2;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        log.info("=== Email Event Initializer Started ===");

        try {
            initializeEmailEvents();
            log.info("=== Email Event Initializer Completed Successfully ===");
        } catch (Exception e) {
            log.error("=== Email Event Initializer Failed ===", e);
        }
    }

    /**
     * Initialize predefined email events
     * Currently only USER_REGISTRATION is implemented
     */
    private void initializeEmailEvents() {
        // Initialize USER_REGISTRATION event with system-defined variables
        initializeEvent(
            "USER_REGISTRATION",
            "Sent when a new user registers in the system. Contains welcome message and account activation instructions."
        );

        // TODO: Add other events in future iterations:
        // - PASSWORD_RESET
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
                log.debug("Email event already exists: {}", eventName);
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
