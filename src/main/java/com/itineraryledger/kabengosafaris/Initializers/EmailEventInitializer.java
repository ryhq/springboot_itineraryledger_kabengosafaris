package com.itineraryledger.kabengosafaris.Initializers;

import com.fasterxml.jackson.databind.ObjectMapper;
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

        /*
         * The one event addressed to a SUPPLIER rather than a customer, and the one written by hand
         * from a safari rather than fired by a state change — which is why its template is worth
         * having here: the wording belongs to the office, not to a deploy.
         */
        initializeEvent(
            "AVAILABILITY_REQUEST",
            "Sent to a lodge or camp asking whether it has room on a safari's nights."
        );

        initializeEvent(
            "AVAILABILITY_REQUEST_CHASE",
            "The follow-up when a property has not answered an availability request."
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

        // ========================================
        // What the PERSON who submitted is told
        //
        // Every event above tells the office something arrived. These tell the sender we heard,
        // which until now nothing did: somebody filled in the planner, got a blank page, and had
        // no way of knowing whether the form had worked at all.
        // ========================================

        initializeEvent(
            "NEWSLETTER_CONFIRM",
            "Sent to the subscriber the moment they submit the newsletter form. Carries the one-click confirmation link; nothing else is ever sent to an address that has not followed it."
        );

        initializeEvent(
            "NEWSLETTER_WELCOME",
            "Sent to the subscriber once they confirm. Says what we will send, how often, and how to leave."
        );

        initializeEvent(
            "CONTACT_US_RECEIVED",
            "Sent back to whoever used the Contact Us form. Quotes their own message so they can see it arrived intact, and names the reply commitment the website makes."
        );

        initializeEvent(
            "BOOKING_INQUIRY_RECEIVED",
            "Sent back to whoever submitted the safari planner. Reads their answers back so a mistake can be corrected before anything is priced, and offers WhatsApp for somebody who would rather not wait."
        );

        // ========================================
        // Quote Events
        // ========================================

        initializeEvent(
            "SEND_QUOTE",
            "Sent to a customer when a safari quote is delivered. Contains quote details, itinerary summary, pricing, validity period, and payment terms."
        );

        // ========================================
        // Safari Lifecycle Events
        // ========================================

        initializeEvent(
            "SAFARI_PAYMENT_GAP",
            "Critical alert sent to operations when a safari has reached its start date but payment is not complete. Requires immediate action."
        );

        initializeEvent(
            "SAFARI_READINESS_ALERT",
            "Warning sent to operations when an upcoming safari has unresolved readiness issues (missing vehicles, accommodations, pax data)."
        );

        initializeEvent(
            "SEND_PAYMENT_ADVICE",
            "Sent to a supplier after we have paid one of their bills: what went out, when, how, "
                + "our transfer reference and what is left owing. Sent by hand from the payment, "
                + "because a correction should not tell them twice."
        );

        initializeEvent(
            "BILL_DUE_REMINDER",
            "Sent as a supplier's bill approaches its due date — a week out, three days out, and on the day itself."
        );

        initializeEvent(
            "SAFARI_STARTED",
            "Notification sent when a safari is automatically started by the system on its start date."
        );

        initializeEvent(
            "SAFARI_COMPLETED",
            "Notification sent when a safari is automatically completed after its end date. Post-trip tasks may be pending."
        );

        initializeEvent(
            "SAFARI_POST_TRIP_REMINDER",
            "Reminder sent for recently completed safaris with pending post-trip tasks (feedback collection, expense reconciliation)."
        );

        // ========================================
        // Safari Customer Communication Events
        // ========================================

        initializeEvent(
            "SEND_SAFARI_DETAILS",
            "Structured email sent to customer with full safari details: dates, itinerary, day-by-day overview. Optional PDF attachment."
        );

        initializeEvent(
            "SEND_SAFARI_MESSAGE",
            "Flexible freeform email sent to customer about their safari. Operator provides subject and message body."
        );

        // ========================================
        // Invoice & Credit Note Events
        // ========================================

        initializeEvent(
            "SEND_INVOICE",
            "Email sent to customer when an invoice is delivered. Includes invoice details, line items summary, total amount, payment terms, and due date."
        );

        initializeEvent(
            "SEND_CREDIT_NOTE",
            "Email sent to customer when a credit note is issued against an invoice. Includes credit details, credited items, total credit amount, and original invoice reference."
        );

        initializeEvent(
            "SEND_PAYMENT_RECEIPT",
            "Email sent to customer when a payment is recorded against an invoice. Includes payment amount, method, reference, invoice details, and remaining balance."
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
            // Get system-defined variables for this event
            String variablesJson = EmailEventVariables.getVariablesForEvent(eventName);

            /*
             * An existing event is RECONCILED, not skipped.
             *
             * The variable catalogue is system-owned: it is generated from the schema files, and
             * the panel says so ("Set by the system, from the code that sends this email"). Only
             * the templates and the on/off switch belong to whoever is using the app, and neither
             * is touched here.
             *
             * Skipping meant a schema could never be corrected after first boot. BOOKING_INQUIRY
             * was seeded when the schema still demanded a variable called `inquiryId`; the schema
             * was later fixed to `inquiryCode`, which is what the sender supplies and what the
             * template prints, but the stored row kept the old name. Every inquiry since then
             * failed validation with "Missing required variables: inquiryId", inside an async
             * block that logs a warning and returns, so nobody was told and no email was sent for
             * any booking inquiry ever received.
             */
            EmailEvent existing = emailEventRepository.findByName(eventName).orElse(null);
            if (existing != null) {
                if (!sameVariables(existing.getVariablesJson(), variablesJson)) {
                    log.warn("Email event {} had a stale variable catalogue; refreshing it from the schema "
                        + "({} variables). Its templates and enabled flag are untouched.",
                        eventName, countVariables(variablesJson));
                    existing.setVariablesJson(variablesJson);
                    emailEventRepository.save(existing);
                } else {
                    log.debug("⊘ Email event already exists and matches its schema: {}", eventName);
                }
                return;
            }

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
     * Whether a stored catalogue still says what the schema says.
     *
     * <p>Compared as parsed JSON rather than as text, so a reformat or a reordered key does not
     * rewrite every event on every boot and bury a real change in the log.
     */
    private boolean sameVariables(String stored, String fromSchema) {
        if (stored == null || stored.isBlank()) return false;
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readTree(stored).equals(mapper.readTree(fromSchema));
        } catch (Exception e) {
            // unreadable stored JSON is exactly the case that needs replacing
            return false;
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
