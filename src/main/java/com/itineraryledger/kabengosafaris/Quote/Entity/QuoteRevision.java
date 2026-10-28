package com.itineraryledger.kabengosafaris.Quote.Entity;

import java.time.LocalDateTime;

import com.itineraryledger.kabengosafaris.ClientPortal.Entity.PortalJourney;
import com.itineraryledger.kabengosafaris.Quote.Enums.RevisionAuthor;
import com.itineraryledger.kabengosafaris.Quote.Enums.RevisionReason;

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
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * The quotation as it stood when the pen changed hands.
 *
 * <p>Written at the handovers, not at every edit. A client moving a lodge, changing their mind and
 * moving it back has changed nothing, and a history that records both moves says the opposite. The
 * rounds of the conversation are what each side remembers and what each side wants to read back.
 *
 * <p>Two of these produce the change list, which is one service feeding both the portal and the
 * panel. The client and the office reading different lists was the failure this exists to prevent:
 * it is how a conversation ends with each side certain the other agreed to something else.
 *
 * <p>The snapshot is JSON and holds no identifiers. See {@code QuoteSnapshot} for why, and
 * {@code TheClientAndTheOfficeSeeTheSameChangesTest} for the guard.
 */
@Entity
@Table(
    name = "quote_revisions",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_quote_revision_number", columnNames = {"quote_id", "revision_number"}),
    indexes = @Index(name = "idx_quote_revision_quote", columnList = "quote_id, id"))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuoteRevision {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quote_id", nullable = false)
    private Quote quote;

    /** How this revision came about, when it came about through a client's link. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journey_id")
    private PortalJourney journey;

    /**
     * 1, 2, 3 within one quotation.
     *
     * <p>What a client is told, so it cannot be the primary key: "version 4" reads as a round of
     * the conversation, and "version 41827" reads as a database.
     */
    @Column(name = "revision_number", nullable = false)
    private Integer revisionNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "taken_by", nullable = false, length = 30)
    private RevisionAuthor takenBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason", nullable = false, length = 40)
    private RevisionReason reason;

    /**
     * The name to show, copied rather than joined.
     *
     * <p>A staff member who leaves must not turn last year's history into a column of nulls, and
     * the name on the record should be the one they had at the time.
     */
    @Column(name = "author_name", length = 200)
    private String authorName;

    @Column(name = "author_user_id")
    private Long authorUserId;

    @Lob
    @Column(name = "snapshot", nullable = false, columnDefinition = "LONGTEXT")
    private String snapshot;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
