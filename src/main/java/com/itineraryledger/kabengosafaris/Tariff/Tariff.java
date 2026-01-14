package com.itineraryledger.kabengosafaris.Tariff;

import com.itineraryledger.kabengosafaris.Activity.ChargingBasis;
import com.itineraryledger.kabengosafaris.ParkTariff.ParkTariff;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Tariff Entity - Defines pricing models/fee types for parks
 *
 * Tariffs are standalone entities that can exist independently.
 * They represent various fee types such as park entry fees, conservation fees,
 * crater service fees, concession fees, etc.
 *
 * A tariff can be linked to multiple parks through the ParkTariff join entity,
 * and rates are defined per park-tariff combination.
 *
 * Key Concepts:
 * - Each tariff has a unique name and a charging basis (PER_PERSON, PER_VEHICLE, PER_GROUP, etc.)
 * - The charging basis determines whether pax age category is required for rate lookup
 * - Tariff rates are defined via ParkTariff -> ParkTariffRate
 *
 * Examples:
 * - "Park Entry Fee" with PER_PERSON basis
 * - "Concession Fee" with PER_VEHICLE basis
 * - "Guide Fee" with PER_GROUP basis
 */
@Entity
@Table(name = "tariffs", indexes = {
    @Index(name = "idx_tariff_name", columnList = "name"),
    @Index(name = "idx_tariff_slug", columnList = "slug"),
    @Index(name = "idx_tariff_charging_basis", columnList = "charging_basis"),
    @Index(name = "idx_tariff_is_active", columnList = "is_active"),
    @Index(name = "idx_tariff_is_system", columnList = "is_system")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tariff {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Unique name for the tariff (case-insensitive uniqueness enforced at service level)
     * Examples: "Park Entry Fee", "Conservation Fee", "Crater Service Fee"
     */
    @Column(nullable = false, unique = true, length = 150)
    private String name;

    /**
     * URL-friendly identifier
     * Auto-generated from name if not provided
     */
    @Column(unique = true, length = 200)
    private String slug;

    /**
     * Determines how this tariff is charged
     * - PER_PERSON: Rate applies per individual passenger (requires age category)
     * - PER_VEHICLE: Rate applies per vehicle (uses highest pax nation priority)
     * - PER_GROUP: Rate applies per booking group (uses highest pax nation priority)
     * - PER_DAY: Rate applies per day
     * - PER_HOUR: Rate applies per hour
     * - PER_SESSION: Rate applies per session
     * - FLAT_RATE: Fixed rate regardless of passengers
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "charging_basis", nullable = false, length = 50)
    @Builder.Default
    private ChargingBasis chargingBasis = ChargingBasis.PER_PERSON;

    /**
     * Detailed description of the tariff
     */
    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * Internal notes for staff (not visible to public/API)
     */
    @Column(name = "internal_notes", columnDefinition = "TEXT")
    private String internalNotes;

    /**
     * Active status - inactive tariffs are excluded from rate calculations
     */
    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    /**
     * Indicates if this is a system tariff (created by initializer)
     * TRUE = System tariff (protected from deletion)
     * FALSE = User-created tariff (can be deleted)
     */
    @Builder.Default
    @Column(name = "is_system", nullable = false)
    private Boolean isSystem = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Parks linked to this tariff through ParkTariff join entity
     */
    @OneToMany(mappedBy = "tariff", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ParkTariff> parkTariffs = new ArrayList<>();

    // ========================
    // HELPER METHODS
    // ========================

    /**
     * Check if this tariff requires pax age category for rate lookup
     * Only PER_PERSON charging basis requires age category
     *
     * @return true if age category is required
     */
    @Transient
    public boolean requiresAgeCategory() {
        return chargingBasis == ChargingBasis.PER_PERSON;
    }

    /**
     * Check if this is a system-protected tariff
     */
    @Transient
    public boolean isSystemTariff() {
        return isSystem != null && isSystem;
    }

    /**
     * Get human-readable charging basis display
     */
    @Transient
    public String getChargingBasisDisplay() {
        return chargingBasis != null ? chargingBasis.getDisplayName() : null;
    }

    // ========================
    // LIFECYCLE HOOKS
    // ========================

    /**
     * Auto-generate slug from name if not provided
     */
    @PrePersist
    @PreUpdate
    private void generateSlug() {
        if (this.slug == null || this.slug.isEmpty()) {
            if (this.name != null) {
                this.slug = this.name
                        .toLowerCase()
                        .replaceAll("[^a-z0-9\\s-]", "")
                        .replaceAll("\\s+", "-")
                        .replaceAll("-+", "-")
                        .trim();
            }
        }
    }
}
