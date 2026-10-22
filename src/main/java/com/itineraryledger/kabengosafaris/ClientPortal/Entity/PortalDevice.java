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
 * A browser that has proved, once, that it can read the participant's inbox.
 *
 * <p>This is the session. It is not a JWT, and it cannot be: this application mints a fresh
 * signing key on every restart, deliberately, so that no token outlives a deploy. That is right
 * for staff and useless for a client whose trip is nine months away, so a portal session is a
 * stored secret with its own lifetime.
 *
 * <p>Only the hash is stored. The cookie value is shown to the browser once and never written
 * down, so a leaked copy of this table is not a leaked set of sessions.
 *
 * <p>A trusted device is safe to the extent that it is visible and killable, which is what the
 * rest of these columns are for. The client sees each one with its address and when it was last
 * used and can end any of them; the office can end all of them; and every new device sends the
 * client a short email saying where and when. That email is the single most valuable control
 * here, because it is how anybody finds out that a forwarded link is being used by somebody else.
 */
@Entity
@Table(name = "portal_devices",
    indexes = @Index(name = "idx_portal_device_participant", columnList = "participant_id"))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortalDevice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "participant_id", nullable = false)
    private PortalParticipant participant;

    /** SHA-256 of the cookie value, hex. The value itself is never stored. */
    @Column(name = "token_hash", nullable = false, length = 64, unique = true)
    private String tokenHash;

    /** Something a person recognises: "Chrome on Windows", not a user agent string. */
    @Column(name = "label", length = 200)
    private String label;

    @Column(name = "ip_address", length = 64)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "last_seen_at")
    private LocalDateTime lastSeenAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (lastSeenAt == null) lastSeenAt = createdAt;
    }

    public boolean isUsable() {
        return revokedAt == null
            && expiresAt != null
            && expiresAt.isAfter(LocalDateTime.now());
    }
}
