package com.itineraryledger.kabengosafaris.EmailAccount.DTOs;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * UpdateEmailAccountDTO - Request DTO for updating email accounts
 *
 * All fields are optional. Only provided fields will be updated.
 * Fields not included in the request will retain their current values.
 *
 * For password updates, always specify the new password explicitly.
 * For isDefault updates, setting to true will automatically set all other accounts to false.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateEmailAccountDTO {

    /**
     * Email address to send from (optional)
     * Must be a valid email format if provided
     */
    @Email(message = "Email must be valid")
    private String email;

    /**
     * Account name - unique identifier for this email account (optional)
     */
    private String name;

    /**
     * Description of what this email account is used for (optional)
     */
    private String description;

    /**
     * SMTP host address (optional)
     */
    private String smtpHost;

    /**
     * SMTP port (optional)
     */
    @Min(value = 1, message = "SMTP port must be greater than 0")
    private Integer smtpPort;

    /**
     * SMTP username for authentication (optional)
     */
    private String smtpUsername;

    /**
     * SMTP password - will be encrypted before storing (optional)
     * Only updated if provided
     */
    private String smtpPassword;

    /**
     * Whether to use TLS (optional)
     */
    private Boolean useTls;

    /**
     * Whether to use SSL (optional)
     */
    private Boolean useSsl;

    /**
     * Whether this Email Account is enabled/active (optional)
     */
    private Boolean enabled;

    /**
     * Whether this is the default Email Account (optional)
     * If set to true, all other accounts will be set to false
     */
    private Boolean isDefault;

    /**
     * Email provider type as integer (optional)
     * 1=GMAIL, 2=OUTLOOK, 3=SENDGRID, 4=MAILGUN, 5=AWS_SES, 6=CUSTOM
     */
    private Integer providerType;

    /**
     * Maximum number of emails to send per minute (optional)
     * 0 = unlimited
     */
    @Min(value = 0, message = "Rate limit cannot be negative")
    private Integer rateLimitPerMinute;

    /**
     * Number of times to retry failed email sending (optional)
     */
    @Min(value = 1, message = "Max retry attempts must be at least 1")
    private Integer maxRetryAttempts;

    /**
     * Delay between retry attempts in seconds (optional)
     */
    @Min(value = 1, message = "Retry delay must be at least 1 second")
    private Integer retryDelaySeconds;
}
