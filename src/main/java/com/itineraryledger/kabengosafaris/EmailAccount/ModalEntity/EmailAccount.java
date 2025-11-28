package com.itineraryledger.kabengosafaris.EmailAccount.ModalEntity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * EmailAccount Entity - Stores email accounts configurations
 * Allows system to send emails from multiple ` email accounts
 *
 * Each accounts includes:
 * - SMTP server details (host, port, protocol)
 * - Authentication credentials (encrypted)
 * - Email account info (email, name)
 * - Security settings (TLS, SSL)
 * - Rate limiting and retry settings
 */

@Entity
@Table(name = "email_accounts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Email address to send from (e.g., noreply@kabengosafaris.com)
     */
    @Column(nullable = false, unique = true)
    private String email;

    /**
     * Email Account name - unique identifier for this email account
     * Examples: "Sales", "Alerts", "Notifications", "Support", "Kabengo Safaris"
     */
    @Column(nullable = false, unique = true)
    private String name;

    /**
     * Description of what this email account is used for
     */
    @Lob
    @Column(length = 1000)
    private String description;

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
     * Whether this Email Account is enabled/active
     */
    @Column(nullable = false)
    private Boolean enabled;

    /**
     * Whether this is the default Email Account to use
     * Only one Email Account should have this set to true
     */
    @Column(nullable = false)
    private Boolean isDefault;

    /**
     * Email provider type for reference (GMAIL, OUTLOOK, CUSTOM, SENDGRID, etc.)
     */
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private EmailAccountProvider providerType;

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
     * Last time this Email Account was successfully tested
     */
    private LocalDateTime lastTestedAt;

    /**
     * Last error message if connection test failed
     */
    @Column(length = 1000)
    private String lastErrorMessage;

    /**
     * Total number of emails sent using this Email Account
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
     * Timestamp when this Email Account was created
     */
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Timestamp when this Email Account was last updated
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
        if (this.enabled == null) this.enabled = false;
        if (this.isDefault == null) this.isDefault = false;
        if (this.useTls == null) this.useTls = true;
        if (this.useSsl == null) this.useSsl = false;
        if (this.rateLimitPerMinute == null) this.rateLimitPerMinute = 0;
        if (this.maxRetryAttempts == null) this.maxRetryAttempts = 3;
        if (this.retryDelaySeconds == null) this.retryDelaySeconds = 5;
        if (this.emailsSentCount == null) this.emailsSentCount = 0L;
        if (this.emailsFailedCount == null) this.emailsFailedCount = 0L;
    }

    @Override
    public String toString() {
        return "EmailAccount{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", smtpHost='" + smtpHost + '\'' +
                ", smtpPort=" + smtpPort +
                ", enabled=" + enabled +
                ", isDefault=" + isDefault +
                ", providerType=" + providerType +
                ", emailsSentCount=" + emailsSentCount +
                ", emailsFailedCount=" + emailsFailedCount +
                '}';
    }
    
}
