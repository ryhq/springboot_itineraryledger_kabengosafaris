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
 * Season Entity - Manages pricing seasons for accommodations
 *
 * Represents seasonal pricing periods (High Season, Low Season, Peak Season, etc.)
 * Each season can have multiple date ranges (SeasonPeriod) to handle:
 * - Multiple non-consecutive periods (e.g., two peak seasons in one year)
 * - Recurring annual seasons
 *
 * IMPROVED from old version:
 * - Uses self-referencing Accommodation (no separate branch entity!)
 * - Better validation
 * - Clearer structure
 * - Helper methods for period management
 */
@Entity
@Table(name = "seasons", indexes = {
    @Index(name = "idx_season_accommodation_id", columnList = "accommodation_id"),
    @Index(name = "idx_season_name", columnList = "name"),
    @Index(name = "idx_season_type", columnList = "season_type"),
    @Index(name = "idx_season_is_active", columnList = "is_active")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Season {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "accommodation_id", nullable = false)
    private Accommodation accommodation;

    @Column(nullable = false, length = 100)
    private String name; // e.g., "High Season", "Low Season", "Peak Season"

    @Enumerated(EnumType.STRING)
    @Column(name = "season_type", length = 50)
    private SeasonType seasonType;

    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

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
