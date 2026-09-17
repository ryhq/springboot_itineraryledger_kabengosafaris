package com.itineraryledger.kabengosafaris.Flight.Entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.itineraryledger.kabengosafaris.Flight.Enums.MarkupType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * An airline we buy seats from, and the markup we sell them at.
 *
 * <p>This is the operator record — the equivalent of an {@code Accommodation}. It holds the things
 * that are true of everything the airline flies rather than of one sector: who they are, what the
 * baggage allowance is, and <strong>what we add to their fare</strong>.
 *
 * <p>The markup lives here because that is where the office wants to set it once. Air Excel is 13%
 * and stays 13% across 243 routes; a fare may override it and a single flight on a single trip may
 * override it again, but neither has to, and nobody retypes 13 two hundred and forty-three times.
 *
 * <p>⚠️ The house rack = 1.3 × STO rule does not apply to this record or anything under it. That is
 * an accommodation rule. A 316 fare marked up 30% is 411 for a seat the airline publishes at 316,
 * which nobody sells; flights go out at cost plus the markup set here.
 */
@Entity
@Table(name = "airlines",
    indexes = {
        @Index(name = "idx_airline_is_active", columnList = "is_active"),
        @Index(name = "idx_airline_code", columnList = "code"),
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_airline_slug", columnNames = "slug"),
        @UniqueConstraint(name = "uk_airline_name", columnNames = "name"),
    })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Airline {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 10)
    private String code;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, length = 160)
    private String slug;

    @Column(length = 255)
    private String website;

    /**
     * Printed for the client. 20 kg on Air Excel's scheduled services, in soft bags — a guest who
     * packs a hard case finds this out at the airstrip otherwise, with the case staying behind.
     */
    @Column(name = "baggage_kg")
    private Integer baggageKg;

    @Column(name = "baggage_notes", columnDefinition = "TEXT")
    private String baggageNotes;

    /** The default for everything this airline flies. Null means no markup unless a line sets one. */
    @Enumerated(EnumType.STRING)
    @Column(name = "markup_type", length = 20)
    private MarkupType markupType;

    @Column(name = "markup_value", precision = 10, scale = 2)
    private BigDecimal markupValue;

    @Column(name = "booking_terms", columnDefinition = "TEXT")
    private String bookingTerms;

    @Column(name = "cancellation_policy", columnDefinition = "TEXT")
    private String cancellationPolicy;

    @Column(name = "internal_notes", columnDefinition = "TEXT")
    private String internalNotes;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /** True when this airline states a markup of its own for a fare or a flight line to inherit. */
    public boolean hasMarkup() {
        return markupType != null && markupValue != null;
    }
}
