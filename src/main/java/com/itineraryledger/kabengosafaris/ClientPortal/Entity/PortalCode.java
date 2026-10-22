package com.itineraryledger.kabengosafaris.ClientPortal.Entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A short code, sent to an inbox, typed into the page the client already has open.
 *
 * <p>Deliberately not a clickable link. A link that logs you in travels through email, and email
 * is full of machines that click things: scanners in corporate mail gateways, preview bots in
 * chat apps, antivirus that fetches every URL to check it. Any of them can spend a one-time link
 * before the client reaches it, and all of them log it. A code typed into a page the client
 * opened themselves is bound to that browser and cannot be spent by a machine that merely read
 * the message.
 *
 * <p>Only the hash is stored, for the same reason as the device token.
 *
 * <p>Six digits is a million, which is nothing without limits and plenty with them. {@link
 * #attempts} caps guessing on a code that has already been issued; the rate limiter caps how many
 * codes can be asked for in the first place.
 */
@Entity
@Table(name = "portal_codes", indexes = {
    @Index(name = "idx_portal_code_participant", columnList = "participant_id"),
    @Index(name = "idx_portal_code_expiry", columnList = "expires_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortalCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "participant_id", nullable = false)
    private PortalParticipant participant;

    /** SHA-256 of the six digits, hex. */
    @Column(name = "code_hash", nullable = false, length = 64)
    private String codeHash;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "attempts", nullable = false)
    @Builder.Default
    private Integer attempts = 0;

    /** Set the moment it is accepted. A code is good once. */
    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /** Where the code was asked for, so an operator can see a pattern of requests. */
    @Column(name = "ip_address", length = 64)
    private String ipAddress;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (attempts == null) attempts = 0;
    }

    public boolean isSpent() {
        return usedAt != null;
    }

    public boolean isExpired() {
        return expiresAt == null || expiresAt.isBefore(LocalDateTime.now());
    }

    public boolean isUsable(int maxAttempts) {
        return !isSpent() && !isExpired() && (attempts == null || attempts < maxAttempts);
    }
}
