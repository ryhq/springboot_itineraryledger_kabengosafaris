package com.itineraryledger.kabengosafaris.EmailAccount.DTOs;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.itineraryledger.kabengosafaris.EmailAccount.ModalEntity.EmailAccountProvider;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * EmailAccountDTO - Data Transfer Object for Email Accounts
 *
 * Transfers email account information to clients with:
 * - Obfuscated ID for security
 * - Sensitive fields excluded (SMTP password never exposed)
 * - Essential configuration and status information
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EmailAccountDTO {

    /**
     * Obfuscated ID encoded using IdObfuscator
     */
    private String id;

    /**
     * Email address to send from
     */
    private String email;

    /**
     * Email Account name - unique identifier
     */
    private String name;

    /**
     * Description of what this email account is used for
     */
    private String description;

    /**
     * SMTP host address
     */
    private String smtpHost;

    /**
     * SMTP port
     */
    private Integer smtpPort;

    /**
     * SMTP username for authentication
     */
    private String smtpUsername;

    /**
     * Whether to use TLS
     */
    private Boolean useTls;

    /**
     * Whether to use SSL
     */
    private Boolean useSsl;

    /**
     * Whether this email account is enabled/active
     */
    private Boolean enabled;

    /**
     * Whether this is the default email account
     */
    private Boolean isDefault;

    /**
     * Email provider type (GMAIL, OUTLOOK, CUSTOM, SENDGRID, etc.)
     */
    private EmailAccountProvider providerType;

    /**
     * Maximum emails to send per minute (0 = unlimited)
     */
    private Integer rateLimitPerMinute;

    /**
     * Number of times to retry failed emails
     */
    private Integer maxRetryAttempts;

    /**
     * Delay between retry attempts in seconds
     */
    private Integer retryDelaySeconds;

    /**
     * Whether to verify SMTP connection on save
     */
    private Boolean verifyOnSave;

    /**
     * Last time this email account was successfully tested
     */
    private LocalDateTime lastTestedAt;

    /**
     * Last error message if connection test failed
     */
    private String lastErrorMessage;

    /**
     * Total number of emails sent using this account
     */
    private Long emailsSentCount;

    /**
     * Total number of failed email attempts
     */
    private Long emailsFailedCount;

    /**
     * Timestamp when this email account was created
     */
    private LocalDateTime createdAt;

    /**
     * Timestamp when this email account was last updated
     */
    private LocalDateTime updatedAt;

    /**
     * User who created this email account (for audit trail)
     */
    private String createdBy;

    /**
     * User who last updated this email account (for audit trail)
     */
    private String updatedBy;
}
