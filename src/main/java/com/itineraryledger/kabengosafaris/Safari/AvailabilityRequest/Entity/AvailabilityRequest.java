package com.itineraryledger.kabengosafaris.Safari.AvailabilityRequest.Entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.itineraryledger.kabengosafaris.Accommodation.Entities.Accommodation;
import com.itineraryledger.kabengosafaris.Safari.Entity.Safari;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One ask to one property: "have you room on these nights?"
 *
 * Sending mail used to leave no trace beyond the Sent folder, so nothing could answer the two
 * questions that actually matter the morning after — has this camp been asked, and did they reply?
 * A request is therefore a record, not a side effect: it names the message that went out, the
 * nights it covered, and what became of it.
 *
 * It covers SEVERAL NIGHTS the way a bill covers several days: through a join table, not a column,
 * because a lodge holding nights 1–2 and 12–13 was asked once about both and re-asking either of
 * them is the mistake this exists to prevent.
 */
@Entity
@Table(
    name = "availability_requests",
    indexes = {
        @Index(name = "idx_availability_request_safari", columnList = "safari_id"),
        @Index(name = "idx_availability_request_accommodation", columnList = "accommodation_id"),
        @Index(name = "idx_availability_request_status", columnList = "status"),
        /* the chase list reads this every morning */
        @Index(name = "idx_availability_request_chase", columnList = "chase_due_at"),
        /* an incoming reply is matched against these two */
        @Index(name = "idx_availability_request_rfc", columnList = "rfc_message_id"),
        @Index(name = "idx_availability_request_thread", columnList = "thread_id")
    })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AvailabilityRequest {

    /**
     * Where the ask has got to.
     *
     * REPLIED means somebody wrote back — NOT that they agreed. "We have space at $400" and "you
     * are booked" are different sentences, and only a person can tell them apart, so the closing
     * reason is set deliberately rather than inferred from a reply arriving.
     */
    public enum Status {
        SENT,
        REPLIED,
        CLOSED
    }

    /** Why an ask is finished with. */
    public enum ClosedReason {
        CONFIRMED,
        DECLINED,
        /** the plan changed and this ask no longer describes the nights */
        SUPERSEDED,
        CANCELLED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "safari_id", nullable = false)
    private Safari safari;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "accommodation_id", nullable = false)
    private Accommodation accommodation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Status status = Status.SENT;

    @Enumerated(EnumType.STRING)
    @Column(name = "closed_reason", length = 20)
    private ClosedReason closedReason;

    /* ------------------------------------------------------------ the mail */

    /**
     * Our own copy of what went out, by obfuscated-decodable id.
     *
     * Kept as a plain id rather than a relation: mail lives under an email ACCOUNT and a safari has
     * no business owning a foreign key into somebody's mailbox. It is what "open the thread" needs.
     */
    @Column(name = "email_message_id")
    private Long emailMessageId;

    @Column(name = "email_account_id")
    private Long emailAccountId;

    /** The RFC Message-ID header, which is what a reply's In-Reply-To will name. */
    @Column(name = "rfc_message_id", length = 400)
    private String rfcMessageId;

    @Column(name = "thread_id", length = 400)
    private String threadId;

    @Column(name = "to_address", length = 320)
    private String toAddress;

    @Column(name = "cc_addresses", length = 2000)
    private String ccAddresses;

    /**
     * Blind copies, kept because they are part of who was told.
     *
     * They are invisible to the recipient and to everyone else on the message — which is exactly
     * why the record has to hold them. Six months on, "who else saw this?" has no other answer.
     */
    @Column(name = "bcc_addresses", length = 2000)
    private String bccAddresses;

    @Column(length = 500)
    private String subject;

    /* ------------------------------------------------------------- timings */

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "sent_by_user_id")
    private Long sentByUserId;

    /** Three working days after it went out — when it stops being "recent" and becomes a chase. */
    @Column(name = "chase_due_at")
    private LocalDateTime chaseDueAt;

    @Column(name = "replied_at")
    private LocalDateTime repliedAt;

    /** The message that answered, once one is matched or linked by hand. */
    @Column(name = "reply_message_id")
    private Long replyMessageId;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @OneToMany(mappedBy = "availabilityRequest", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<AvailabilityRequestStay> stays = new ArrayList<>();

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    /** Open means still waiting on the property — the state the anti-spam guard asks about. */
    public boolean isOpen() {
        return status != Status.CLOSED;
    }
}
