package com.itineraryledger.kabengosafaris.Quote.Entity;

import java.time.LocalDateTime;

import com.itineraryledger.kabengosafaris.ClientPortal.Entity.PortalJourney;
import com.itineraryledger.kabengosafaris.Quote.Enums.CommentAnchor;
import com.itineraryledger.kabengosafaris.Quote.Enums.RevisionAuthor;

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
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One thing somebody said about one part of a quotation.
 *
 * <p>A review that can only be accepted or rejected is not a review. The anchor is the feature:
 * "day 3, Kudu Lodge: too far from the gate for an early start" is actionable in a way that
 * "not quite right" is not, and the difference between them is three emails.
 *
 * <p><strong>{@code visibleToClient} is a security boundary, not a display preference.</strong>
 * False means the office talking among themselves on the same thread, which is exactly where
 * somebody will write what we think the customer can afford. The portal's read path filters in
 * the query rather than in a template, because a template is edited by somebody thinking about
 * layout.
 *
 * <p>A client's own comment is always visible. There is nowhere for a client to keep a secret
 * from the only other party to the conversation, so offering the option would be a lie.
 */
@Entity
@Table(name = "quote_proposal_comments", indexes = {
    @Index(name = "idx_proposal_comment_quote", columnList = "quote_id, id"),
    @Index(name = "idx_proposal_comment_anchor", columnList = "quote_id, day_number")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuoteProposalComment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quote_id", nullable = false)
    private Quote quote;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journey_id")
    private PortalJourney journey;

    /**
     * The round it was written against.
     *
     * <p>So a point answered two versions ago reads as history rather than as an outstanding
     * objection, which is how a resolved conversation comes to look like an unhappy customer.
     */
    @Column(name = "revision_number")
    private Integer revisionNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "anchor", nullable = false, length = 20)
    private CommentAnchor anchor;

    /**
     * The day, by number rather than by row.
     *
     * <p>Day rows are replaced wholesale when staff rebuild a stretch of a trip. A comment about
     * day four is about day four whichever row is carrying it this week.
     */
    @Column(name = "day_number")
    private Integer dayNumber;

    /**
     * The line's real primary key, never the obfuscated form.
     *
     * <p>This installation mints a fresh Hashids salt on every restart unless a deployment fixes
     * one, so an obfuscated id written here today addresses a different row next week, or
     * nothing at all.
     */
    @Column(name = "target_id")
    private Long targetId;

    /**
     * What that line was called when the comment was written.
     *
     * <p>Denormalised like {@code authorName}, and for the same kind of reason: "too far from
     * the gate" against a row somebody has since deleted is a sentence nobody can act on.
     */
    @Column(name = "target_label", length = 255)
    private String targetLabel;

    @Column(name = "body", nullable = false, columnDefinition = "TEXT")
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(name = "author_kind", nullable = false, length = 20)
    private RevisionAuthor authorKind;

    @Column(name = "author_name", length = 200)
    private String authorName;

    @Column(name = "author_user_id")
    private Long authorUserId;

    @Column(name = "visible_to_client", nullable = false)
    @Builder.Default
    private Boolean visibleToClient = true;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "resolved_by", length = 200)
    private String resolvedBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (visibleToClient == null) visibleToClient = true;
        if (anchor == null) anchor = CommentAnchor.WHOLE;
    }

    public boolean isResolved() {
        return resolvedAt != null;
    }
}
