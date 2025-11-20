package com.itineraryledger.kabengosafaris.EmailConfiguration;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * EmailConfiguration Entity - Stores email provider configurations
 * Allows system to send emails from multiple configured email accounts
 *
 * Each configuration includes:
 * - SMTP server details (host, port, protocol)
 * - Authentication credentials (encrypted)
 * - Email account info (from address, display name)
 * - Security settings (TLS, SSL)
 * - Rate limiting and retry settings
 */
@Entity
@Table(name = "email_configurations")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailConfiguration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Configuration name - unique identifier for this email config
     * Examples: "default", "alerts", "notifications", "support"
     */
    @Column(nullable = false, unique = true)
    private String name;

    /**
     * Human-readable display name for this configuration
     * Examples: "Default Email", "Alert Notifications"
     */
    @Column(nullable = false)
    private String displayName;

    /**
     * Description of what this configuration is used for
     */
    @Lob
    @Column(length = 1000)
    private String description;

    /**
     * Email address to send from (e.g., noreply@kabengosafaris.com)
     */
    @Column(nullable = false)
    private String fromEmail;

    /**
     * Display name for the sender (e.g., "Kabengo Safaris")
     */
    @Column(nullable = false)
    private String fromName;

    /**
     * SMTP host address (e.g., smtp.gmail.com, smtp.outlook.com)
     */
    @Column(nullable = false)
    private String smtpHost;

    /**
     * SMTP port (typically 25, 465, 587, 2525)
     */
    @Column(nullable = false)
    private Integer smtpPort;

    /**
     * Email account username for SMTP authentication
     */
    @Column(nullable = false)
    private String smtpUsername;

    /**
     * Email account password - ENCRYPTED in database
     * Password is encrypted using standard encryption before storing
     */
    @Column(nullable = false)
    private String smtpPassword;

    /**
     * Whether to use TLS (Transport Layer Security)
     * Usually true for secure connections on ports 25, 587
     */
    @Column(nullable = false)
    private Boolean useTls;

    /**
     * Whether to use SSL (Secure Sockets Layer)
     * Usually true for secure connections on port 465
     */
    @Column(nullable = false)
    private Boolean useSsl;

    /**
     * Whether this configuration is enabled/active
     */
    @Column(nullable = false)
    private Boolean enabled;

    /**
     * Whether this is the default configuration to use
     * Only one configuration should have this set to true
     */
    @Column(nullable = false)
    private Boolean isDefault;

    /**
     * Email provider type for reference (GMAIL, OUTLOOK, CUSTOM, SENDGRID, etc.)
     */
    @Column(nullable = false)
    private String providerType;

    /**
     * Maximum number of emails to send per minute (rate limiting)
     * 0 = unlimited
     */
    @Column(nullable = false)
    private Integer rateLimitPerMinute;

    /**
     * Number of times to retry failed email sending
     */
    @Column(nullable = false)
    private Integer maxRetryAttempts;

    /**
     * Delay between retry attempts in seconds
     */
    @Column(nullable = false)
    private Integer retryDelaySeconds;

    /**
     * Whether to verify SMTP connection on configuration creation/update
     */
    @Column(nullable = false)
    private Boolean verifyOnSave;

    /**
     * Last time this configuration was successfully tested
     */
    private LocalDateTime lastTestedAt;

    /**
     * Last error message if connection test failed
     */
    @Column(length = 1000)
    private String lastErrorMessage;

    /**
     * Total number of emails sent using this configuration
     */
    @Column(nullable = false)
    @Builder.Default
    private Long emailsSentCount = 0L;

    /**
     * Total number of failed email attempts
     */
    @Column(nullable = false)
    @Builder.Default
    private Long emailsFailedCount = 0L;

    /**
     * Timestamp when this configuration was created
     */
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Timestamp when this configuration was last updated
     */
    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Created by user (optional - for audit trail)
     */
    private String createdBy;

    /**
     * Last updated by user (optional - for audit trail)
     */
    private String updatedBy;

    @PrePersist
    protected void onCreate() {
        if (this.enabled == null) this.enabled = true;
        if (this.isDefault == null) this.isDefault = false;
        if (this.useTls == null) this.useTls = true;
        if (this.useSsl == null) this.useSsl = false;
        if (this.rateLimitPerMinute == null) this.rateLimitPerMinute = 0;
        if (this.maxRetryAttempts == null) this.maxRetryAttempts = 3;
        if (this.retryDelaySeconds == null) this.retryDelaySeconds = 5;
        if (this.verifyOnSave == null) this.verifyOnSave = false;
        if (this.emailsSentCount == null) this.emailsSentCount = 0L;
        if (this.emailsFailedCount == null) this.emailsFailedCount = 0L;
    }

    @Override
    public String toString() {
        return "EmailConfiguration{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", displayName='" + displayName + '\'' +
                ", fromEmail='" + fromEmail + '\'' +
                ", smtpHost='" + smtpHost + '\'' +
                ", smtpPort=" + smtpPort +
                ", enabled=" + enabled +
                ", isDefault=" + isDefault +
                ", providerType='" + providerType + '\'' +
                ", emailsSentCount=" + emailsSentCount +
                ", emailsFailedCount=" + emailsFailedCount +
                '}';
    }
}
