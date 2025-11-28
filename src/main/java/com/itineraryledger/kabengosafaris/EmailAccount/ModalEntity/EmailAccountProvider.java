package com.itineraryledger.kabengosafaris.EmailAccount.ModalEntity;

/**
 * EmailAccountProvider Enum - Defines supported email provider types
 *
 * Supported providers:
 * - GMAIL: Google Gmail SMTP
 * - OUTLOOK: Microsoft Outlook/Office 365 SMTP
 * - SENDGRID: SendGrid email service
 * - MAILGUN: Mailgun email service
 * - AWS_SES: Amazon SES (Simple Email Service)
 * - CUSTOM: Custom SMTP server
 */
public enum EmailAccountProvider {
    GMAIL("Gmail"),
    OUTLOOK("Outlook/Office 365"),
    SENDGRID("SendGrid"),
    MAILGUN("Mailgun"),
    AWS_SES("Amazon SES"),
    CUSTOM("Custom SMTP");

    private final String displayName;

    EmailAccountProvider(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Get provider by name (case-insensitive)
     */
    public static EmailAccountProvider fromString(String name) {
        try {
            return EmailAccountProvider.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return CUSTOM; // Default to CUSTOM if provider not found
        }
    }
}
