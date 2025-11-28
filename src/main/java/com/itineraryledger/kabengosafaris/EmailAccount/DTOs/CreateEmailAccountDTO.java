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
     */
    @NotBlank(message = "SMTP host is required")
    private String smtpHost;

    /**
     * SMTP port (typically 25, 465, 587, 2525)
     */
    @NotNull(message = "SMTP port is required")
    @Min(value = 1, message = "SMTP port must be greater than 0")
    private Integer smtpPort;

    /**
     * SMTP username for authentication
     */
    @NotBlank(message = "SMTP username is required")
    private String smtpUsername;

    /**
     * SMTP password - will be encrypted before storing
     */
    @NotBlank(message = "SMTP password is required")
    private String smtpPassword;

    /**
     * Whether to use TLS (Transport Layer Security)
     * Usually true for secure connections on ports 25, 587
     */
    @NotNull(message = "useTls is required")
    private Boolean useTls;

    /**
     * Whether to use SSL (Secure Sockets Layer)
     * Usually true for secure connections on port 465
     */
    @NotNull(message = "useSsl is required")
    private Boolean useSsl;

    /**
     * Email provider type as integer
     * 1=GMAIL, 2=OUTLOOK, 3=SENDGRID, 4=MAILGUN, 5=AWS_SES, 6=CUSTOM
     */
    @NotNull(message = "Provider type is required")
    private Integer providerType;

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
    
}
