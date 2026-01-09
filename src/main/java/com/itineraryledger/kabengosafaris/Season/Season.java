package com.itineraryledger.kabengosafaris.Season;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.itineraryledger.kabengosafaris.Accommodation.Entities.Accommodation;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Season Entity - Manages pricing seasons for accommodations AND global seasons
 *
 * Represents seasonal pricing periods (High Season, Low Season, Peak Season, etc.)
 * Each season can have multiple date ranges (SeasonPeriod) to handle:
 * - Multiple non-consecutive periods (e.g., two peak seasons in one year)
 * - Recurring annual seasons
 *
 * SUPPORTS TWO TYPES OF SEASONS:
 * 1. Accommodation-specific seasons: accommodation_id is set (not null)
 * 2. Global seasons: accommodation_id is null (used by parks for general pricing)
 *
 * Features:
 * - Flexible ownership (can be accommodation-specific or global)
 * - Better validation
 * - Clearer structure
 * - Helper methods for period management
 * - Scope detection (isGlobal() helper method)
 */
@Entity
@Table(name = "seasons", indexes = {
    @Index(name = "idx_season_accommodation_id", columnList = "accommodation_id"),
    @Index(name = "idx_season_name", columnList = "name"),
    @Index(name = "idx_season_type", columnList = "season_type"),
    @Index(name = "idx_season_is_active", columnList = "is_active"),
    @Index(name = "idx_season_is_global", columnList = "is_global")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Season {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Optional: Links this season to a specific accommodation
     * If NULL, this is a GLOBAL season (used by parks, general pricing, etc.)
     * If NOT NULL, this is an accommodation-specific season
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "accommodation_id", nullable = true)
    private Accommodation accommodation;

    @Column(nullable = false, length = 100)
    private String name; // e.g., "High Season", "Low Season", "Peak Season"

    @Enumerated(EnumType.STRING)
    @Column(name = "season_type", length = 50)
    private SeasonType seasonType;

    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * Indicates if this is a global season (not tied to any accommodation)
     * TRUE = Global season (for parks, general use)
     * FALSE = Accommodation-specific season
     * This field is redundant with (accommodation_id == null) but improves query performance
     */
    @Builder.Default
    @Column(name = "is_global", nullable = false)
    private Boolean isGlobal = false;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    /**
     * Indicates if this is a system season (created by initializer)
     * TRUE = System season (protected from deletion, created by GlobalSeasonInitializer)
     * FALSE = User-created season (can be deleted)
     * System seasons and their periods can only be updated, never deleted
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

    // Relationship with SeasonPeriod
    @OneToMany(mappedBy = "season", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<SeasonPeriod> seasonPeriods = new ArrayList<>();

    // Helper methods for managing season periods
    public void addSeasonPeriod(SeasonPeriod period) {
        seasonPeriods.add(period);
        period.setSeason(this);
    }

    public void removeSeasonPeriod(SeasonPeriod period) {
        seasonPeriods.remove(period);
        period.setSeason(null);
    }

    /**
     * Check if this is a global season (not tied to any accommodation)
     */
    @Transient
    public boolean isGlobalSeason() {
        return isGlobal != null && isGlobal;
    }

    /**
     * Check if this is an accommodation-specific season
     */
    @Transient
    public boolean isAccommodationSpecific() {
        return !isGlobalSeason() && accommodation != null;
    }

    /**
     * Check if this is a system season (protected from deletion)
     */
    @Transient
    public boolean isSystemSeason() {
        return isSystem != null && isSystem;
    }

    /**
     * Lifecycle hook to ensure isGlobal consistency
     */
    @PrePersist
    @PreUpdate
    protected void ensureGlobalConsistency() {
        if (accommodation == null) {
            isGlobal = true;
        } else {
            isGlobal = false;
        }
    }

    /**
     * Season types for categorization
     */
    public enum SeasonType {
        HIGH_SEASON("High Season", "High demand period with premium pricing"),
        LOW_SEASON("Low Season", "Off-peak period with reduced pricing"),
        PEAK_SEASON("Peak Season", "Highest demand period with maximum pricing"),
        SHOULDER_SEASON("Shoulder Season", "Moderate demand period between peak and low"),
        FESTIVE_SEASON("Festive Season", "Special holiday and festive periods"),
        SPECIAL_EVENT("Special Event", "Specific events or occasions"),
        STANDARD("Standard", "Regular year-round pricing"),
        CUSTOM("Custom", "Custom defined season");

        private final String displayName;
        private final String description;

        SeasonType(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getDescription() {
            return description;
        }
    }
}
