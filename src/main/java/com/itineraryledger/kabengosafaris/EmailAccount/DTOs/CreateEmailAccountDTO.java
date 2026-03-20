package com.itineraryledger.kabengosafaris.EmailAccount.DTOs;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * CreateEmailAccountDTO - Request DTO for creating new email accounts
 *
 * Validates all required fields for email account creation:
 * - SMTP configuration (host, port, username, password)
 * - Email account details (email, name)
 * - Security settings (TLS, SSL)
 * - Rate limiting and retry settings
 * - Provider type
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateEmailAccountDTO {

    /**
     * Email address to send from
     * Must be a valid email format
     */
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    /**
     * Account name - unique identifier for this email account
     * Examples: "Sales", "Alerts", "Notifications", "Support"
     */
    @NotBlank(message = "Account name is required")
    private String name;

    /**
     * Description of what this email account is used for
     */
    private String description;

    /**
     * SMTP host address (e.g., smtp.gmail.com, smtp.outlook.com)
     * Required for SMTP-based providers, optional for API-based providers (Resend, SendGrid)
     */
    private String smtpHost;

    /**
     * SMTP port (typically 25, 465, 587, 2525)
     * Required for SMTP-based providers, optional for API-based providers
     */
    @Min(value = 1, message = "SMTP port must be greater than 0")
    private Integer smtpPort;

    /**
     * SMTP username for authentication
     * Required for SMTP-based providers, optional for API-based providers
     */
    private String smtpUsername;

    /**
     * SMTP password - will be encrypted before storing
     * Required for SMTP-based providers, optional for API-based providers
     */
    private String smtpPassword;

    /**
     * Whether to use TLS (Transport Layer Security)
     * Usually true for secure connections on ports 25, 587
     */
    private Boolean useTls;

    /**
     * Whether to use SSL (Secure Sockets Layer)
     * Usually true for secure connections on port 465
     */
    private Boolean useSsl;

    /**
     * Email provider type as integer
     * 1=GMAIL, 2=OUTLOOK, 3=SENDGRID, 4=MAILGUN, 5=AWS_SES, 6=CUSTOM, 7=RESEND
     */
    @NotNull(message = "Provider type is required")
    private Integer providerType;

    /**
     * API key for HTTP-based email providers (Resend, SendGrid, etc.)
     * Required for API-based providers (providerType=7), optional otherwise
     */
    private String apiKey;

    /**
     * Webhook signing secret for verifying webhook callbacks
     * Optional - used by providers that support webhooks (Resend, SendGrid)
     */
    private String webhookSecret;

    /**
     * Sending method as integer
     * 1=API, 2=SMTP
     * For API-based providers (Resend), defaults to API (1)
     * For SMTP-based providers, always SMTP (2)
     */
    private Integer sendingMethod;

    /**
     * Maximum number of emails to send per minute (rate limiting)
     * 0 = unlimited
     */
    @NotNull(message = "Rate limit per minute is required")
    @Min(value = 0, message = "Rate limit cannot be negative")
    private Integer rateLimitPerMinute;

    /**
     * Number of times to retry failed email sending
     */
    @NotNull(message = "Max retry attempts is required")
    @Min(value = 1, message = "Max retry attempts must be at least 1")
    private Integer maxRetryAttempts;

    /**
     * Delay between retry attempts in seconds
     */
    @NotNull(message = "Retry delay seconds is required")
    @Min(value = 1, message = "Retry delay must be at least 1 second")
    private Integer retryDelaySeconds;

    // ---- Receiving Configuration (optional) ----

    /**
     * Receiving protocol: 1=IMAP, 2=POP3, 3=NONE (default NONE)
     */
    private Integer receivingProtocol;

    /**
     * IMAP/POP3 host address (e.g., mail.kabengosafaris.com)
     */
    private String imapHost;

    /**
     * IMAP/POP3 port (e.g., 993 for IMAPS, 995 for POP3S)
     */
    @Min(value = 1, message = "IMAP port must be greater than 0")
    private Integer imapPort;

    /**
     * Whether to use SSL for IMAP/POP3
     */
    private Boolean imapUseSsl;

    /**
     * Whether to use STARTTLS for IMAP/POP3
     */
    private Boolean imapUseTls;

    /**
     * How often to fetch emails in minutes
     */
    @Min(value = 1, message = "Fetch interval must be at least 1 minute")
    private Integer fetchIntervalMinutes;

    /**
     * Maximum emails to fetch per cycle
     */
    @Min(value = 1, message = "Max fetch count must be at least 1")
    private Integer maxFetchCount;
}
