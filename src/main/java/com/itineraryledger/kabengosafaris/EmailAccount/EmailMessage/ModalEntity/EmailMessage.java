package com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.ModalEntity;

import java.time.LocalDateTime;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.itineraryledger.kabengosafaris.EmailAccount.ModalEntity.EmailAccount;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "email_messages", indexes = {
    @Index(name = "idx_email_msg_account_id", columnList = "email_account_id"),
    @Index(name = "idx_email_msg_folder_id", columnList = "folder_id"),
    @Index(name = "idx_email_msg_message_id", columnList = "message_id"),
    @Index(name = "idx_email_msg_thread_id", columnList = "thread_id"),
    @Index(name = "idx_email_msg_sent_at", columnList = "sent_at"),
    @Index(name = "idx_email_msg_is_read", columnList = "is_read"),
    @Index(name = "idx_email_msg_is_flagged", columnList = "is_flagged"),
    @Index(name = "idx_email_msg_snooze_until", columnList = "snooze_until"),
    @Index(name = "idx_email_msg_from_address", columnList = "from_address")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "email_account_id", nullable = false)
    private EmailAccount emailAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "folder_id", nullable = false)
    private EmailFolder folder;

    /**
     * RFC 2822 Message-ID header
     */
    @Column(name = "message_id")
    private String messageId;

    /**
     * In-Reply-To header (parent Message-ID for threading)
     */
    private String inReplyTo;

    /**
     * Comma-separated Message-IDs for threading (References header)
     */
    @Column(name = "`references`", length = 2000)
    private String references;

    /**
     * Computed thread identifier for grouping related messages
     */
    @Column(name = "thread_id")
    private String threadId;

    @Column(name = "from_address")
    private String fromAddress;

    private String fromName;

    /**
     * JSON array of recipient email addresses
     */
    @Column(length = 2000)
    private String toAddresses;

    /**
     * JSON array of CC email addresses
     */
    @Column(length = 2000)
    private String ccAddresses;

    /**
     * JSON array of BCC email addresses
     */
    @Column(length = 2000)
    private String bccAddresses;

    private String subject;

    /**
     * First ~200 chars of plain text body for list preview
     */
    @Column(length = 500)
    private String snippet;

    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private Boolean isRead = false;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isStarred = false;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isDraft = false;

    @Column(nullable = false)
    @Builder.Default
    private Boolean hasAttachments = false;

    @Column(nullable = false)
    @Builder.Default
    private Integer attachmentCount = 0;

    @Column(name = "is_flagged", nullable = false)
    @Builder.Default
    private Boolean isFlagged = false;

    /**
     * If set in the future, this message is hidden from inbox views until that
     * timestamp passes. Cleared by EmailSnoozeWakeJob. See EMAIL_INBOX_API.md
     * §3 (snooze) on the frontend side.
     */
    @Column(name = "snooze_until")
    private LocalDateTime snoozeUntil;

    /**
     * Reference to .eml file on disk. Null for messages sent via the Resend
     * API (and other API-only senders) — those have no on-disk MIME copy.
     * Null is only valid when storagePath is also "sent" / no read-back of
     * the original .eml is expected.
     */
    @Column
    private String fileName;

    /**
     * Size of the .eml file in bytes
     */
    private Long fileSize;

    /**
     * Relative storage path: {accountId}/{folderName}/
     */
    private String storagePath;

    /**
     * Email Date header (when the email was originally sent)
     */
    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    /**
     * When this email was fetched/received into the system
     */
    private LocalDateTime receivedAt;

    /**
     * IMAP UID for incremental sync
     */
    private String imapUid;

    @OneToMany(mappedBy = "emailMessage", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<EmailAttachment> attachments;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (this.isRead == null) this.isRead = false;
        if (this.isStarred == null) this.isStarred = false;
        if (this.isDraft == null) this.isDraft = false;
        if (this.hasAttachments == null) this.hasAttachments = false;
        if (this.attachmentCount == null) this.attachmentCount = 0;
        if (this.isFlagged == null) this.isFlagged = false;
    }
}
