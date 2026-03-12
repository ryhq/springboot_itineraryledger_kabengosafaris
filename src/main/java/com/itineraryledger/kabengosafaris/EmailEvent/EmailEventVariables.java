package com.itineraryledger.kabengosafaris.EmailEvent;

import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Defines system-wide variables for each email event.
 * These variables are immutable and cannot be modified via API.
 * Template creators MUST use these exact variable names in their templates.
 *
 * Variable Format:
 * - name: The variable name used in templates as {{variableName}}
 * - description: Human-readable description of what the variable represents
 * - isRequired: Whether the variable must be provided when sending emails
 * - defaultValue: Optional default value if not provided
 */
public class EmailEventVariables {

    /**
     * Cache for loaded schemas to avoid repeated file reads
     */
    private static final Map<String, String> SCHEMA_CACHE = new HashMap<>();

    /**
     * Get the system-defined variables for a specific email event
     *
     * @param eventName The name of the email event (e.g., "USER_REGISTRATION")
     * @return JSON string containing variable definitions
     */
    public static String getVariablesForEvent(String eventName) {
        return switch (eventName) {
            case "USER_REGISTRATION" -> loadSchema("user-registration-schema.json");
            case "PASSWORD_RESET" -> loadSchema("password-reset-schema.json");
            case "BACKUP_SUCCESS" -> loadSchema("backup-success-schema.json");
            case "BACKUP_FAILURE" -> loadSchema("backup-failure-schema.json");
            case "NEWSLETTER_SUBSCRIPTION" -> loadSchema("newsletter-subscription-schema.json");
            case "BOOKING_INQUIRY" -> loadSchema("booking-inquiry-schema.json");
            case "CONTACT_US" -> loadSchema("contact-us-schema.json");
            default -> "[]";
        };
    }

    /**
     * Get the display name for an email event
     */
    public static String getDisplayName(String eventName) {
        return switch (eventName) {
            case "USER_REGISTRATION" -> "User Registration";
            case "PASSWORD_RESET" -> "Password Reset";
            case "BACKUP_SUCCESS" -> "Backup Success";
            case "BACKUP_FAILURE" -> "Backup Failure";
            case "NEWSLETTER_SUBSCRIPTION" -> "Newsletter Subscription";
            case "BOOKING_INQUIRY" -> "Booking Inquiry";
            case "CONTACT_US" -> "Contact Us";
            default -> eventName;
        };
    }

    /**
     * Get the description for an email event
     */
    public static String getDescription(String eventName) {
        return switch (eventName) {
            case "USER_REGISTRATION" -> "Email sent when a new user registers an account. Includes account activation link.";
            case "PASSWORD_RESET" -> "Email sent when a user requests to reset their password. Includes password reset link.";
            case "BACKUP_SUCCESS" -> "Email notification sent when a backup completes successfully. Includes backup details and download link.";
            case "BACKUP_FAILURE" -> "Email alert sent when a backup operation fails. Includes error details and last successful backup information.";
            case "NEWSLETTER_SUBSCRIPTION" -> "Admin notification sent when someone subscribes to the newsletter. Includes subscriber details.";
            case "BOOKING_INQUIRY" -> "Admin notification sent when a new booking inquiry is submitted. Includes full inquiry details and traveler information.";
            case "CONTACT_US" -> "Admin notification sent when someone submits a message through the Contact Us form. Includes sender details and message content.";
            default -> "";
        };
    }

    /**
     * Get all supported email event names
     *
     * @return Array of email event names that have defined variables
     */
    public static String[] getSupportedEvents() {
        return new String[]{
            "USER_REGISTRATION",
            "PASSWORD_RESET",
            "BACKUP_SUCCESS",
            "BACKUP_FAILURE",
            "NEWSLETTER_SUBSCRIPTION",
            "BOOKING_INQUIRY",
            "CONTACT_US"
        };
    }

    /**
     * Load schema from JSON file in classpath resources
     *
     * @param filename The schema filename (e.g., "user-registration-schema.json")
     * @return JSON string containing the schema
     */
    private static String loadSchema(String filename) {
        // Check cache first
        if (SCHEMA_CACHE.containsKey(filename)) {
            return SCHEMA_CACHE.get(filename);
        }

        try {
            ClassPathResource resource = new ClassPathResource("schemas/email-events/" + filename);
            String schema = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

            // Cache the schema for future use
            SCHEMA_CACHE.put(filename, schema);

            return schema;
        } catch (IOException e) {
            throw new RuntimeException("Failed to load email event schema: " + filename, e);
        }
    }

}
