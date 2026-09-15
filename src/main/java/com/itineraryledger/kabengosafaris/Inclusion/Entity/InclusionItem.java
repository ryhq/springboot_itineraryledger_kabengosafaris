package com.itineraryledger.kabengosafaris.Inclusion.Entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.itineraryledger.kabengosafaris.User.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One line of the promise attached to a price — "All park, conservation and crater-service fees".
 *
 * <p>Before this existed, that sentence lived as free text on the itinerary, and the same eight
 * lines were pasted into 58 of 65 itineraries. Correcting a word meant editing 58 records, three
 * itineraries had never been pasted into at all, and no document printed any of it.
 *
 * <p>The catalogue holds the wording once. A document — itinerary, quote, safari, invoice — holds
 * rows that point at these, each row saying whether this trip includes the thing or does not. The
 * side is a property of the row, not of the item: "Optional activities" is not-included on a
 * standard safari and included on one where we have thrown a balloon flight in.
 */
@Entity
@Table(name = "inclusion_items",
    indexes = {
        @Index(name = "idx_inclusion_item_is_active", columnList = "is_active"),
        @Index(name = "idx_inclusion_item_display_order", columnList = "display_order"),
        @Index(name = "idx_inclusion_item_category", columnList = "category"),
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_inclusion_item_code", columnNames = {"code"})
    })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InclusionItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The only stable name this has outside its row id.
     *
     * <p>The house delete contract answers {@code skipped:[{id, code, reason}]}, and a refused row
     * has to be nameable in that report. Also what the company-to-company export matches on.
     */
    @Column(name = "code", length = 60)
    private String code;

    /**
     * The sentence a customer reads, verbatim.
     *
     * <p>VARCHAR rather than TEXT: it is one line, it is searched with LIKE, and it is the key the
     * seeder and the backfill match on. The longest of the fourteen house lines is 51 characters.
     */
    @Column(name = "label", nullable = false, length = 300)
    private String label;

    /**
     * A heading to group by, free text exactly as {@code Faq.category} is.
     *
     * <p>Nothing branches on it. An enum would cost a migration every time somebody wants a new
     * heading and would buy nothing, because the field that IS read by code is claimAppliesTo.
     */
    @Column(name = "category", length = 120)
    private String category;

    /** The print order, catalogue-wide. A new row's link inherits this as its starting position. */
    @Builder.Default
    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;

    /**
     * Enabled or disabled.
     *
     * <p>Disabled means: hidden from the picker, refused by the write endpoints, and filtered out
     * of live reads. It does NOT reach back into a document already sent — those hold their own
     * copy of the wording as text, on purpose.
     */
    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    /** Seeded by us and protected from deletion, the convention {@code PaxAgeCategory} already uses. */
    @Builder.Default
    @Column(name = "is_system", nullable = false)
    private Boolean isSystem = false;

    /**
     * Whether a new itinerary starts with a row for this.
     *
     * <p>The fourteen house lines are standard; something like "Scuba diving in Zanzibar" is not,
     * so it appears on neither list of a mainland trip until somebody adds it. Without this, every
     * item added to the catalogue would appear on every trip's paperwork.
     */
    @Builder.Default
    @Column(name = "is_standard", nullable = false)
    private Boolean isStandard = true;

    /** Which side a standard row starts on — eight of the house lines true, six false. */
    @Builder.Default
    @Column(name = "default_included", nullable = false)
    private Boolean defaultIncluded = true;

    /**
     * What this sentence claims about the priced lines, as {@code LineCategoryScope} canonical text.
     *
     * <p><strong>NULL MEANS NO CLAIM, which is the opposite of what the shared helper assumes.</strong>
     * {@code LineCategoryScope.covers(null, type)} returns true and {@code parse(null, …)} returns
     * every category, because for tax and discount an unset scope means "the whole quote". Here an
     * unset scope means this sentence says nothing a machine can check — "Drinking water on game
     * drives" is not a claim about a QuoteItemType. Anything reading this field must short-circuit
     * on null BEFORE calling either helper, or every quote is accused of every contradiction.
     */
    @Column(name = "claim_applies_to", length = 200)
    private String claimAppliesTo;

    /** For the office: when to tick it, what it does not cover. Never printed. */
    @Column(name = "internal_notes", columnDefinition = "TEXT")
    private String internalNotes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by")
    private User updatedBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /** Derived from the row id once it has one, like every other coded record here. */
    public String generateCode() {
        return id == null ? null : String.format("INC-%05d", id);
    }

    public boolean isSystemItem() {
        return isSystem != null && isSystem;
    }

    /** True when this sentence claims something a priced document can be checked against. */
    public boolean makesACheckableClaim() {
        return claimAppliesTo != null && !claimAppliesTo.isBlank();
    }
}
