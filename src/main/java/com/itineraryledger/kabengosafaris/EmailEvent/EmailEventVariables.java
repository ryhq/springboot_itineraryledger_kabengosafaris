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
            case "SEND_QUOTE" -> loadSchema("quote-sent-schema.json");
            case "SAFARI_PAYMENT_GAP" -> loadSchema("safari-payment-gap-schema.json");
            case "SAFARI_READINESS_ALERT" -> loadSchema("safari-readiness-alert-schema.json");
            case "SAFARI_STARTED" -> loadSchema("safari-started-schema.json");
            case "SAFARI_COMPLETED" -> loadSchema("safari-completed-schema.json");
            case "SAFARI_POST_TRIP_REMINDER" -> loadSchema("safari-post-trip-reminder-schema.json");
            case "SEND_SAFARI_DETAILS" -> loadSchema("safari-details-schema.json");
            case "SEND_SAFARI_MESSAGE" -> loadSchema("safari-customer-message-schema.json");
            case "SEND_INVOICE" -> loadSchema("invoice-sent-schema.json");
            case "SEND_CREDIT_NOTE" -> loadSchema("credit-note-sent-schema.json");
            case "SEND_PAYMENT_RECEIPT" -> loadSchema("payment-receipt-sent-schema.json");
            case "AVAILABILITY_REQUEST" -> loadSchema("availability-request-schema.json");
            case "AVAILABILITY_REQUEST_CHASE" -> loadSchema("availability-request-chase-schema.json");
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
            case "SEND_QUOTE" -> "Quote Sent to Customer";
            case "AVAILABILITY_REQUEST" -> "Availability Request to a Property";
            case "AVAILABILITY_REQUEST_CHASE" -> "Availability Request — Chase";
            case "SAFARI_PAYMENT_GAP" -> "Safari Payment Gap Alert";
            case "SAFARI_READINESS_ALERT" -> "Safari Readiness Alert";
            case "SAFARI_STARTED" -> "Safari Auto-Started";
            case "SAFARI_COMPLETED" -> "Safari Auto-Completed";
            case "SAFARI_POST_TRIP_REMINDER" -> "Safari Post-Trip Reminder";
            case "SEND_SAFARI_DETAILS" -> "Safari Details to Customer";
            case "SEND_SAFARI_MESSAGE" -> "Safari Custom Message to Customer";
            case "SEND_INVOICE" -> "Invoice Sent to Customer";
            case "SEND_CREDIT_NOTE" -> "Credit Note Sent to Customer";
            case "SEND_PAYMENT_RECEIPT" -> "Payment Receipt Sent to Customer";
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
            case "SEND_QUOTE" -> "Email sent to a customer when a safari quote is delivered. Includes quote details, itinerary summary, pricing, validity period, and payment terms.";
            case "AVAILABILITY_REQUEST_CHASE" -> "The follow-up when a property has not answered an availability request. Shorter than the first letter: they already have the detail, so this says when we wrote, how long we have waited, and asks for an answer either way.";
            case "AVAILABILITY_REQUEST" -> "Sent to a lodge or camp asking whether it has room on a safari's nights. Unlike every other event here it goes to a SUPPLIER, not a customer, and it is written from the safari rather than triggered by a state change.";
            case "SAFARI_PAYMENT_GAP" -> "Critical alert sent to operations when a safari has reached its start date but payment is not complete. Requires immediate action.";
            case "SAFARI_READINESS_ALERT" -> "Warning sent to operations when an upcoming safari has unresolved readiness issues (missing vehicles, accommodations, pax data).";
            case "SAFARI_STARTED" -> "Notification sent when a safari is automatically started by the system on its start date.";
            case "SAFARI_COMPLETED" -> "Notification sent when a safari is automatically completed after its end date. Post-trip tasks may be pending.";
            case "SAFARI_POST_TRIP_REMINDER" -> "Reminder sent for recently completed safaris with pending post-trip tasks (feedback collection, expense reconciliation).";
            case "SEND_SAFARI_DETAILS" -> "Structured email sent to customer with full safari details: dates, itinerary summary, day-by-day overview, and special requests. Optional PDF attachment.";
            case "SEND_SAFARI_MESSAGE" -> "Flexible freeform email sent to customer about their safari. Operator provides the subject and message body for any safari-related communication.";
            case "SEND_INVOICE" -> "Email sent to customer when an invoice is delivered. Includes invoice details, line items summary, total amount, payment terms, and due date.";
            case "SEND_CREDIT_NOTE" -> "Email sent to customer when a credit note is issued against an invoice. Includes credit details, credited items, total credit amount, and original invoice reference.";
            case "SEND_PAYMENT_RECEIPT" -> "Email sent to customer when a payment is recorded against an invoice. Includes payment amount, method, reference, invoice details, and remaining balance.";
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
            "CONTACT_US",
            "SEND_QUOTE",
            "AVAILABILITY_REQUEST",
            "AVAILABILITY_REQUEST_CHASE",
            "SAFARI_PAYMENT_GAP",
            "SAFARI_READINESS_ALERT",
            "SAFARI_STARTED",
            "SAFARI_COMPLETED",
            "SAFARI_POST_TRIP_REMINDER",
            "SEND_SAFARI_DETAILS",
            "SEND_SAFARI_MESSAGE",
            "SEND_INVOICE",
            "SEND_CREDIT_NOTE",
            "SEND_PAYMENT_RECEIPT"
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
