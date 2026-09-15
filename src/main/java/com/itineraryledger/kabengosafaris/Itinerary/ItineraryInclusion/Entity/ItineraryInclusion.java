package com.itineraryledger.kabengosafaris.Itinerary.ItineraryInclusion.Entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.itineraryledger.kabengosafaris.Inclusion.Entity.InclusionItem;
import com.itineraryledger.kabengosafaris.Itinerary.Entity.Itinerary;

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
 * One line of an itinerary's promise: a catalogue item, and whether this trip includes it.
 *
 * <p>An entity rather than a {@code @ManyToMany} join, because it carries payload. Every
 * association in this codebase that says something of its own — {@code ItineraryPax},
 * {@code ItineraryDayParkTariff} — is an entity; the four {@code @ManyToMany}s are all payload-free
 * tag sets.
 *
 * <p>{@code isIncluded} is stored rather than derived. "Not included" as the complement of the
 * ticked set would mean adding one row to the catalogue silently rewrote the not-included paragraph
 * of every itinerary at once — a sentence sent to a paying customer that nobody wrote and nobody
 * reviewed. It would also make it impossible to add a line at quote level, which is the whole point
 * of the chain.
 */
@Entity
@Table(name = "itinerary_inclusions",
    indexes = {
        @Index(name = "idx_itinerary_inclusion_itinerary", columnList = "itinerary_id"),
        @Index(name = "idx_itinerary_inclusion_item", columnList = "inclusion_item_id"),
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_itinerary_inclusion",
            columnNames = {"itinerary_id", "inclusion_item_id"})
    })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItineraryInclusion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "itinerary_id", nullable = false)
    private Itinerary itinerary;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inclusion_item_id", nullable = false)
    private InclusionItem inclusionItem;

    /** True prints it under "what's included", false under "what's not included". */
    @Builder.Default
    @Column(name = "is_included", nullable = false)
    private Boolean isIncluded = true;

    /**
     * Print order, on the row rather than read through the catalogue.
     *
     * <p>Seeded from the item's {@code displayOrder} when the row is created. Reading the order
     * through the join instead would mean dragging one item in the catalogue silently reordered the
     * printed promise on all 65 itineraries, which is an edit nobody made.
     */
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
