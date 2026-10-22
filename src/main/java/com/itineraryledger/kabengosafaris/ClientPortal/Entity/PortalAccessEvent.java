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
 * What a client opened, and when.
 *
 * <p>Two honest purposes. Support: "she says the invoice is not there" is answerable in seconds
 * if you can see that she opened the trip four times and the invoice never. And insight: where
 * people stall, how long a quotation sits before it is accepted, which section is read twice.
 *
 * <p>This is <strong>personal data about an identified individual</strong>, and a good share of
 * these people are in the EU. It is therefore the one table in this module with a deletion date:
 * {@code PortalRetentionScheduler} empties it three months after the safari ends, or three months
 * after the booking falls through. The quotation and the invoice are financial records and stay;
 * somebody's browsing history is not a financial record.
 *
 * <p>There is no device fingerprint here beyond the address and the user agent, and there is no
 * MAC address, because a web page cannot read one. Anything claiming otherwise is guessing.
 */
@Entity
@Table(name = "portal_access_events",
    indexes = @Index(name = "idx_portal_event_journey", columnList = "journey_id, created_at"))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortalAccessEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "journey_id", nullable = false)
    private PortalJourney journey;

    /** Null before anybody has verified: the open tier is readable without identifying yourself. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "participant_id")
    private PortalParticipant participant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id")
    private PortalDevice device;

    /** TRIP, QUOTE, INVOICE, DOCUMENT, DEVICES, CHANGES, VERIFY, and the actions. */
    @Column(name = "section", nullable = false, length = 60)
    private String section;

    /** Which document, which day. Never free text from the client. */
    @Column(name = "detail", length = 200)
    private String detail;

    @Column(name = "ip_address", length = 64)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
