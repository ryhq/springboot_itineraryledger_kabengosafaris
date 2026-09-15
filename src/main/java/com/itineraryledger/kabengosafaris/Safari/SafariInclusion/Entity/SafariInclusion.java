package com.itineraryledger.kabengosafaris.Safari.SafariInclusion.Entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.itineraryledger.kabengosafaris.Inclusion.Entity.InclusionItem;
import com.itineraryledger.kabengosafaris.Safari.Entity.Safari;

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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A line of the promise the booked trip was sold on.

 * <p>Copied from the QUOTE the customer accepted, not from the itinerary template — those two
 * disagree the moment anybody edits the quote, and what the customer agreed to is the quote.
 *
 * <p><strong>The wording is stored here as text, not joined through the catalogue.</strong> One
 * {@code UPDATE inclusion_items SET label = …} would otherwise rewrite every document ever sent,
 * including ones already rendered to PDF and emailed — so the customer's copy and ours would
 * disagree, and ours would be the one that looked altered. {@code inclusionItem} survives only as
 * a breadcrumb for the panel, nullable, and is never dereferenced to render anything.
 *
 * <p>It is also what the rest of this codebase already does with money:
 * {@code QuoteItem.itemName} and {@code InvoiceLineItem.itemName} are denormalised text for
 * exactly this reason.
 */
@Entity
@Table(name = "safari_inclusions", indexes = {
    @Index(name = "idx_safari_inclusions_safari", columnList = "safari_id"),
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SafariInclusion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "safari_id", nullable = false)
    private Safari safari;

    /** Where this line came from. Null once that catalogue row is gone; nothing reads it to render. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inclusion_item_id")
    private InclusionItem inclusionItem;

    /** The sentence as it was when this document was produced. This is what prints. */
    @Column(name = "label", nullable = false, length = 300)
    private String label;

    @Column(name = "category", length = 120)
    private String category;

    /**
     * The claim this line made, snapshotted too.
     *
     * <p>Read live, narrowing a catalogue item's scope would change which contradictions a sent
     * quote reports — so a warning somebody cleared before sending would reappear afterwards,
     * about a document they can no longer change.
     */
    @Column(name = "claim_applies_to", length = 200)
    private String claimAppliesTo;

    @Builder.Default
    @Column(name = "is_included", nullable = false)
    private Boolean isIncluded = true;

    @Builder.Default
    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public boolean included() {
        return isIncluded != null && isIncluded;
    }
}
