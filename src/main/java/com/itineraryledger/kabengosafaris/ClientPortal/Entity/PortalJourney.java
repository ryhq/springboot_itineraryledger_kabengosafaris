package com.itineraryledger.kabengosafaris.ClientPortal.Entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.itineraryledger.kabengosafaris.ClientPortal.Enums.EditingLevel;
import com.itineraryledger.kabengosafaris.ClientPortal.Enums.JourneyStatus;
import com.itineraryledger.kabengosafaris.ClientPortal.Enums.ProposalState;
import com.itineraryledger.kabengosafaris.Customer.Entity.Customer;
import com.itineraryledger.kabengosafaris.Invoice.Entity.Invoice;
import com.itineraryledger.kabengosafaris.Itinerary.Entity.Itinerary;
import com.itineraryledger.kabengosafaris.Quote.Entity.Quote;
import com.itineraryledger.kabengosafaris.Safari.Entity.Safari;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One client's trip, behind one link, from enquiry to safari.
 *
 * <p>A journey ties a customer to an itinerary and to whichever of the quotation, invoice and
 * safari exist so far. The client opens one address and finds all of it, instead of searching
 * their inbox for which email held which PDF.
 *
 * <p><strong>The token is an address, not a credential.</strong> It says which booking a page is
 * about. On its own it opens the trip and nothing else; the quotation, the invoice and anything
 * personal need a code sent to an address already on the customer record. That split is the whole
 * security design, and it exists because links leak: forwarded to a sister, pasted into a group
 * chat, unfurled by a preview bot, left in the history of a hotel computer. Assume the URL is
 * public and design so that it does not matter.
 *
 * <p>The token is its own random secret and never an obfuscated id. Hashids are an encoding of the
 * primary key, not a secret, and this application mints a fresh salt on every restart, so an id
 * is both guessable and unstable. {@code APortalLinkIsNotACredentialTest} fails the build if this
 * ever changes.
 *
 * <p>Three independent questions live here and are deliberately three fields. {@link #status} is
 * whether the link works at all; {@link #proposalState} is who is holding the pen; the quote's own
 * status is where the paperwork stands. Collapsing any two of them locks a client out of a trip
 * they are still going on.
 */
@Entity
@Table(name = "portal_journeys", indexes = {
    @Index(name = "idx_portal_journey_customer", columnList = "customer_id"),
    @Index(name = "idx_portal_journey_quote", columnList = "quote_id"),
    @Index(name = "idx_portal_journey_purge", columnList = "purge_after")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortalJourney {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The address in the link. 32 random bytes, url-safe Base64, unique.
     *
     * <p>Never a Hashid. See the class note.
     */
    @Column(name = "token", nullable = false, length = 64, unique = true)
    private String token;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "itinerary_id")
    private Itinerary itinerary;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quote_id")
    private Quote quote;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id")
    private Invoice invoice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "safari_id")
    private Safari safari;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private JourneyStatus status;

    /**
     * How far this client may reach into their own quotation.
     *
     * <p>Null means inherit: the itinerary's level if it has one, otherwise the company default.
     * Set here, it overrides both, which is how one suspicious request is narrowed without
     * touching the product or anybody else's journey.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "editing_level", length = 30)
    private EditingLevel editingLevel;

    /**
     * Frozen by the office. Nothing can be proposed, accepted or declined.
     *
     * <p>Separate from revoking, which closes the link entirely. A locked journey can still be
     * read, which is the right answer when something looks wrong but the client may be innocent.
     */
    @Column(name = "locked", nullable = false)
    @Builder.Default
    private Boolean locked = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "proposal_state", nullable = false, length = 30)
    private ProposalState proposalState;

    @Column(name = "proposal_state_at")
    private LocalDateTime proposalStateAt;

    /** The staff member currently holding the pen, when one is. */
    @Column(name = "held_by_user_id")
    private Long heldByUserId;

    /**
     * The revision that was on the table when we took it over.
     *
     * <p>An edit arriving from a page the client already had open is refused by comparing against
     * this, rather than quietly overwriting what somebody is in the middle of reviewing.
     */
    @Column(name = "held_from_revision")
    private Long heldFromRevision;

    /**
     * When the behavioural data for this journey may be destroyed.
     *
     * <p>Three months after the safari ends, or three months after the booking falls through.
     * Written when the journey closes so the retention job never has to re-derive it from four
     * different documents, and so a change to one of those documents cannot silently extend how
     * long somebody's browsing history is kept.
     */
    @Column(name = "purge_after")
    private LocalDate purgeAfter;

    @OneToMany(mappedBy = "journey", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<PortalParticipant> participants = new ArrayList<>();

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /** After this, the link stops opening. Null means it lives as long as the journey does. */
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @Column(name = "revoked_reason", length = 500)
    private String revokedReason;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
        if (status == null) status = JourneyStatus.OPEN;
        if (proposalState == null) proposalState = ProposalState.OPEN_TO_CLIENT;
        if (proposalStateAt == null) proposalStateAt = createdAt;
        if (locked == null) locked = false;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /** Whether the link opens anything at all right now. */
    public boolean isOpenable() {
        if (status == null || !status.opensAnything()) return false;
        return expiresAt == null || expiresAt.isAfter(LocalDateTime.now());
    }

    /**
     * Whether this client may change or decide anything right now.
     *
     * <p>One predicate rather than three scattered checks, because every caller has to ask the
     * same question and a caller that asks a different one is a hole.
     */
    public boolean isActionable() {
        return isOpenable()
            && status.allowsAction()
            && !Boolean.TRUE.equals(locked);
    }

    /** The lead traveller, who is the only participant allowed to decide anything. */
    public PortalParticipant lead() {
        if (participants == null) return null;
        return participants.stream()
            .filter(p -> p.getParticipantRole() != null && p.getParticipantRole().mayAct())
            .findFirst()
            .orElse(null);
    }
}
