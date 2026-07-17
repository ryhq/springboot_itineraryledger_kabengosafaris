package com.itineraryledger.kabengosafaris.Itinerary.Entity;

import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.Entity.ItineraryDay;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryPax.Entity.ItineraryPax;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Itinerary Entity - Safari itinerary template/skeleton
 *
 * Represents a reusable safari itinerary that includes:
 * - Day-by-day breakdown of activities, parks, and accommodations
 * - Passenger categories with counts
 * - Status workflow (DRAFT -> COMPLETE -> PUBLISHED)
 */
@Entity
@Table(name = "itineraries", indexes = {
    @Index(name = "idx_itinerary_name", columnList = "name"),
    @Index(name = "idx_itinerary_code", columnList = "code"),
    @Index(name = "idx_itinerary_status", columnList = "status"),
    @Index(name = "idx_itinerary_trip_type", columnList = "trip_type"),
    @Index(name = "idx_itinerary_budget_category", columnList = "budget_category"),
    @Index(name = "idx_itinerary_is_active", columnList = "is_active"),
    @Index(name = "idx_itinerary_created_by", columnList = "created_by")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Itinerary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name; // e.g., "7-Day Serengeti & Ngorongoro Safari"

    @Column(unique = true, length = 100)
    private String code; // e.g., "ITI-7D6N-001" - auto-generated

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    private ItineraryStatus status = ItineraryStatus.DRAFT;

    @Enumerated(EnumType.STRING)
    @Column(name = "trip_type", length = 50)
    private TripType tripType; // PRIVATE, GROUP, CUSTOM, etc.

    @Enumerated(EnumType.STRING)
    @Column(name = "budget_category", length = 50)
    private BudgetCategory budgetCategory; // LUXURY, MID_RANGE, BUDGET, etc.

    @Min(value = 1, message = "Total days must be at least 1")
    @Column(name = "total_days", nullable = false)
    private Integer totalDays;

    @Min(value = 0, message = "Total nights must be zero or more")
    @Column(name = "total_nights", nullable = false)
    private Integer totalNights;

    @Min(value = 1, message = "Car count must be at least 1")
    @Column(name = "car_count", nullable = false)
    @Builder.Default
    private Integer carCount = 1;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String description; // Rich description of the itinerary

    @Lob
    @Column(columnDefinition = "TEXT")
    private String highlights; // Key highlights (JSON array format)

    @Column(name = "start_location", length = 200)
    private String startLocation; // e.g., "Arusha"

    @Column(name = "end_location", length = 200)
    private String endLocation; // e.g., "Arusha"

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Builder.Default
    @Column(name = "featured") // nullable so ddl-auto can ADD COLUMN on tables with existing rows (strict-mode safe)
    private Boolean featured = false; // editor-curated: shown in the public "featured" strip / "popular" sort

    @Column(name = "from_price_usd")
    private Double fromPriceUsd; // denormalized per-person "from" rack price (USD) for public sort/filter

    @Column(name = "created_by")
    private Long createdBy; // User ID who created this itinerary

    @Column(name = "primary_image_url", length = 500)
    private String primaryImageUrl; // Admin-set primary image URL for public views

    @Lob
    @Column(name = "internal_notes", columnDefinition = "TEXT")
    private String internalNotes; // Staff notes, not visible to customers

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // ========================
    // RELATIONSHIPS
    // ========================

    @OneToMany(mappedBy = "itinerary", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @OrderBy("dayNumber ASC")
    private List<ItineraryDay> days = new ArrayList<>();

    @OneToMany(mappedBy = "itinerary", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ItineraryPax> paxList = new ArrayList<>();

    // ========================
    // HELPER METHODS
    // ========================

    public void addDay(ItineraryDay day) {
        days.add(day);
        day.setItinerary(this);
    }

    public void removeDay(ItineraryDay day) {
        days.remove(day);
        day.setItinerary(null);
    }

    public void addPax(ItineraryPax pax) {
        paxList.add(pax);
        pax.setItinerary(this);
    }

    public void removePax(ItineraryPax pax) {
        paxList.remove(pax);
        pax.setItinerary(null);
    }

    /**
     * Get total passenger count across all categories
     */
    @Transient
    public int getTotalPaxCount() {
        return paxList.stream()
            .mapToInt(pax -> pax.getCount() != null ? pax.getCount() : 0)
            .sum();
    }

    /**
     * Check if itinerary meets minimum requirements for publishing
     */
    @Transient
    public boolean canPublish() {
        return totalDays != null && totalDays > 0
            && !days.isEmpty()
            && days.size() == totalDays
            && !paxList.isEmpty();
    }

    /**
     * Get display name for the itinerary (days and nights format)
     */
    @Transient
    public String getDaysNightsDisplay() {
        return totalDays + " Days / " + totalNights + " Nights";
    }

    /**
     * Generate itinerary code based on days, nights, and ID
     * Format: ITI-{days}D{nights}N-{100 + id}
     */
    @Transient
    public String generateCode() {
        if (id == null || totalDays == null || totalNights == null) {
            return null;
        }
        return String.format("ITI-%dD%dN-%03d", totalDays, totalNights, 1000 + id);
    }

    @PrePersist
    @PreUpdate
    private void validate() {
        // Auto-calculate nights if not set
        if (totalNights == null && totalDays != null) {
            totalNights = Math.max(0, totalDays - 1);
        }
        // Ensure car count is at least 1
        if (carCount == null || carCount < 1) {
            carCount = 1;
        }
    }

    // ========================
    // ENUM
    // ========================

    public enum ItineraryStatus {
        DRAFT("Draft", "Itinerary is being created or edited"),
        COMPLETE("Complete", "All required data is filled in"),
        PUBLISHED("Published", "Itinerary is available for booking"),
        ARCHIVED("Archived", "Itinerary is no longer in active use");

        private final String displayName;
        private final String description;

        ItineraryStatus(String displayName, String description) {
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
