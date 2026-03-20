package com.itineraryledger.kabengosafaris.EmailAccount.ResendWebhook;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entity for storing Resend webhook events.
 * Each record represents a single webhook callback from Resend.
 */
@Entity
@Table(name = "resend_webhook_events", indexes = {
    @Index(name = "idx_resend_webhook_svix_id", columnList = "svixId", unique = true),
    @Index(name = "idx_resend_webhook_email_id", columnList = "emailId")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResendWebhookEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Unique event ID from Resend (svix-id header) — used for idempotency
     */
    @Column(nullable = false, unique = true)
    private String svixId;

    /**
     * Event type (e.g., email.sent, email.delivered, email.bounced, email.complained)
     */
    @Column(nullable = false)
    private String eventType;

    /**
     * Resend email ID (e.g., re_123456)
     */
    private String emailId;

    /**
     * Sender email address from the event
     */
    private String fromEmail;

    /**
     * Recipient email address from the event
     */
    private String toEmail;

    /**
     * Email subject from the event
     */
    private String subject;

    /**
     * Full JSON payload for debugging and auditing
     */
    @Lob
    @Column(columnDefinition = "TEXT")
    private String rawPayload;

    /**
     * Timestamp of the event as reported by Resend
     */
    private LocalDateTime eventTimestamp;

    /**
     * Timestamp when we received and stored this event
     */
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime receivedAt;
}
