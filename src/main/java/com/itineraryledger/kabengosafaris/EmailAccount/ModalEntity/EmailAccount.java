package com.itineraryledger.kabengosafaris.EmailAccount.ModalEntity;

import java.time.LocalDateTime;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.itineraryledger.kabengosafaris.EmailAccount.EmailAccountSignatures.ModalEntity.EmailAccountSignature;

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
     * Nullable for API-based providers (Resend, SendGrid)
     */
    private String smtpHost;

    /**
     * SMTP port (typically 25, 465, 587, 2525)
     * Nullable for API-based providers (Resend, SendGrid)
     */
    private Integer smtpPort;

    /**
     * Email account username for SMTP authentication
     * Nullable for API-based providers (Resend, SendGrid)
     */
    private String smtpUsername;

    /**
     * Email account password - ENCRYPTED in database
     * Password is encrypted using standard encryption before storing
     * Nullable for API-based providers (Resend, SendGrid)
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

    // ---- Receiving Configuration ----

    /**
     * Protocol for receiving emails (IMAP, POP3, or NONE)
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ReceivingProtocol receivingProtocol = ReceivingProtocol.NONE;

    /**
     * IMAP/POP3 host address (e.g., mail.kabengosafaris.com)
     */
    private String imapHost;

    /**
     * IMAP/POP3 port (e.g., 993 for IMAPS, 995 for POP3S)
     */
    private Integer imapPort;

    /**
     * Whether to use SSL for IMAP/POP3 connection
     */
    private Boolean imapUseSsl;

    /**
     * Whether to use STARTTLS for IMAP/POP3 connection
     */
    private Boolean imapUseTls;

    /**
     * Whether email receiving is enabled for this account
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean receivingEnabled = false;

    /**
     * How often to fetch emails in minutes
     */
    @Builder.Default
    private Integer fetchIntervalMinutes = 5;

    /**
     * Maximum number of emails to fetch per cycle
     */
    @Builder.Default
    private Integer maxFetchCount = 50;

    /**
     * Last time emails were successfully fetched
     */
    private LocalDateTime lastFetchedAt;

    /**
     * Last fetch error message
     */
    @Column(length = 1000)
    private String lastFetchErrorMessage;

    /**
     * Consecutive fetch failure count (resets on success, used for backoff)
     */
    @Column(nullable = false)
    @Builder.Default
    private Integer consecutiveFetchFailures = 0;

    /**
     * Total number of emails received
     */
    @Column(nullable = false)
    @Builder.Default
    private Long emailsReceivedCount = 0L;

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
     * Email provider type for reference (GMAIL, OUTLOOK, CUSTOM, SENDGRID, RESEND, etc.)
     */
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private EmailAccountProvider providerType;

    /**
     * API key for HTTP-based email providers (Resend, SendGrid, etc.)
     * Encrypted using EncryptionUtil before storing
     */
    @Column(name = "api_key")
    private String apiKey;

    /**
     * Webhook signing secret for verifying webhook callbacks (per-account)
     * Encrypted using EncryptionUtil before storing
     */
    @Column(name = "webhook_secret")
    private String webhookSecret;

    /**
     * Sending method for this email account (API or SMTP)
     * API-based providers (Resend) default to API but can use SMTP gateway
     * SMTP-based providers always use SMTP
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "sending_method", nullable = false)
    @Builder.Default
    private SendingMethod sendingMethod = SendingMethod.SMTP;

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
     * One-to-Many relationship with EmailSignature
     * When an email account is deleted, all associated signatures are also deleted
     * FetchType.LAZY ensures signatures are not loaded by default
     * CascadeType.ALL ensures cascading delete, persist, update operations
     */
    @OneToMany(mappedBy = "emailAccount", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<EmailAccountSignature> signatures;

    /**
     * Whether to include signature in all emails sent from this account if the signature is present
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean includeSignatureByDefault = true;

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
        if (this.includeSignatureByDefault == null) this.includeSignatureByDefault = true;
        if (this.receivingProtocol == null) this.receivingProtocol = ReceivingProtocol.NONE;
        if (this.receivingEnabled == null) this.receivingEnabled = false;
        if (this.fetchIntervalMinutes == null) this.fetchIntervalMinutes = 5;
        if (this.maxFetchCount == null) this.maxFetchCount = 50;
        if (this.emailsReceivedCount == null) this.emailsReceivedCount = 0L;
        if (this.sendingMethod == null) this.sendingMethod = SendingMethod.SMTP;
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
                ", includeSignatureByDefault=" + includeSignatureByDefault +
                ", emailsSentCount=" + emailsSentCount +
                ", emailsFailedCount=" + emailsFailedCount +
                '}';
    }
    
}
