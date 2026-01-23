package com.itineraryledger.kabengosafaris.Safari.SafariDay.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.itineraryledger.kabengosafaris.Safari.Entity.Safari;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayAccommodation.Entity.SafariDayAccommodation;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayActivity.Entity.SafariDayActivity;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayPark.Entity.SafariDayPark;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * SafariDay Entity - Represents a single day in a safari booking
 *
 * Copied from ItineraryDay when Safari is created from Itinerary template.
 *
 * Key differences from ItineraryDay:
 * - Has an actual date (not just day number)
 * - Can track day-specific status and modifications
 * - Links to SafariDayActivity, SafariDayPark, SafariDayAccommodation
 *
 * Safari-specific additions:
 * - actualDate: The actual calendar date for this day
 * - isModified: Flag indicating changes from original itinerary
 * - modificationNotes: Notes about what was modified
 * - weatherNotes: Weather observations for the day
 * - actualStartTime: When activities actually started
 * - actualEndTime: When activities actually ended
 */
@Entity
@Table(name = "safari_days",
    indexes = {
        @Index(name = "idx_safari_day_safari_id", columnList = "safari_id"),
        @Index(name = "idx_safari_day_number", columnList = "day_number"),
        @Index(name = "idx_safari_day_actual_date", columnList = "actual_date")
    },
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_safari_day_number",
            columnNames = {"safari_id", "day_number"}
        )
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SafariDay {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "safari_id", nullable = false)
    @JsonIgnore
    private Safari safari;

    @Column(name = "day_number", nullable = false)
    private Integer dayNumber;

    @Column(name = "day_tag", length = 50)
    private String dayTag; // e.g., "Day 1", "Day 2"

    @Column(nullable = false, length = 200)
    private String title; // e.g., "Arrival in Arusha"

    // ========================
    // SAFARI-SPECIFIC: ACTUAL DATE
    // ========================

    /**
     * The actual calendar date for this safari day.
     * Calculated from Safari.startDate + (dayNumber - 1)
     */
    @Column(name = "actual_date", nullable = false)
    private LocalDate actualDate;

    // ========================
    // DESCRIPTION FIELDS
    // ========================

    @Lob
    @Column(columnDefinition = "TEXT")
    private String description; // Main day description/overview

    @Lob
    @Column(name = "morning_activities", columnDefinition = "TEXT")
    private String morningActivities; // Morning activities description

    @Lob
    @Column(name = "afternoon_activities", columnDefinition = "TEXT")
    private String afternoonActivities; // Afternoon activities description

    @Lob
    @Column(name = "evening_activities", columnDefinition = "TEXT")
    private String eveningActivities; // Evening activities description

    @Lob
    @Column(name = "wildlife_highlights", columnDefinition = "TEXT")
    private String wildlifeHighlights; // Wildlife to expect

    @Lob
    @Column(name = "scenic_highlights", columnDefinition = "TEXT")
    private String scenicHighlights; // Scenic views and landscapes

    @Lob
    @Column(name = "special_notes", columnDefinition = "TEXT")
    private String specialNotes; // Special tips or notes for the day

    // ========================
    // LOCATION & TRAVEL
    // ========================

    @Column(name = "start_location", length = 200)
    private String startLocation;

    @Column(name = "end_location", length = 200)
    private String endLocation;

    @Column(name = "distance_km")
    private Integer distanceKm; // Driving distance in kilometers

    // ========================
    // DAY SETTINGS
    // ========================

    @Builder.Default
    @Column(name = "is_overnight", nullable = false)
    private Boolean isOvernight = true; // False for last day / departure

    @Column(name = "meals_included", length = 100)
    private String mealsIncluded; // e.g., "B,L,D" or "Breakfast, Lunch, Dinner"

    @Lob
    @Column(name = "internal_notes", columnDefinition = "TEXT")
    private String internalNotes;

    // ========================
    // SAFARI-SPECIFIC FIELDS
    // ========================

    /**
     * Flag indicating this day was modified from original itinerary
     */
    @Builder.Default
    @Column(name = "is_modified", nullable = false)
    private Boolean isModified = false;

    /**
     * Notes about modifications made to this day
     */
    @Lob
    @Column(name = "modification_notes", columnDefinition = "TEXT")
    private String modificationNotes;

    /**
     * Weather observations or notes for the day
     */
    @Lob
    @Column(name = "weather_notes", columnDefinition = "TEXT")
    private String weatherNotes;

    /**
     * Actual time when day's activities started
     */
    @Column(name = "actual_start_time", length = 10)
    private String actualStartTime;

    /**
     * Actual time when day's activities ended
     */
    @Column(name = "actual_end_time", length = 10)
    private String actualEndTime;

    /**
     * Driver/guide notes for this specific day
     */
    @Lob
    @Column(name = "driver_notes", columnDefinition = "TEXT")
    private String driverNotes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // ========================
    // RELATIONSHIPS
    // ========================

    @OneToMany(mappedBy = "safariDay", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @OrderBy("sortOrder ASC")
    private List<SafariDayActivity> activities = new ArrayList<>();

    @OneToMany(mappedBy = "safariDay", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @OrderBy("sortOrder ASC")
    private List<SafariDayPark> parks = new ArrayList<>();

    @OneToMany(mappedBy = "safariDay", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<SafariDayAccommodation> accommodations = new ArrayList<>();

    // ========================
    // HELPER METHODS
    // ========================

    public void addActivity(SafariDayActivity activity) {
        activities.add(activity);
        activity.setSafariDay(this);
    }

    public void removeActivity(SafariDayActivity activity) {
        activities.remove(activity);
        activity.setSafariDay(null);
    }

    public void addPark(SafariDayPark park) {
        parks.add(park);
        park.setSafariDay(this);
    }

    public void removePark(SafariDayPark park) {
        parks.remove(park);
        park.setSafariDay(null);
    }

    public void addAccommodation(SafariDayAccommodation accommodation) {
        accommodations.add(accommodation);
        accommodation.setSafariDay(this);
    }

    public void removeAccommodation(SafariDayAccommodation accommodation) {
        accommodations.remove(accommodation);
        accommodation.setSafariDay(null);
    }

    /**
     * Check if this day has any parks scheduled
     */
    @Transient
    public boolean hasParks() {
        return parks != null && !parks.isEmpty();
    }

    /**
     * Check if this day has any activities scheduled
     */
    @Transient
    public boolean hasActivities() {
        return activities != null && !activities.isEmpty();
    }

    /**
     * Check if this day has accommodation assigned
     */
    @Transient
    public boolean hasAccommodation() {
        return accommodations != null && !accommodations.isEmpty();
    }

    /**
     * Check if this day is in the past
     */
    @Transient
    public boolean isPast() {
        return actualDate != null && LocalDate.now().isAfter(actualDate);
    }

    /**
     * Check if this day is today
     */
    @Transient
    public boolean isToday() {
        return actualDate != null && LocalDate.now().equals(actualDate);
    }

    /**
     * Check if this day is in the future
     */
    @Transient
    public boolean isFuture() {
        return actualDate != null && LocalDate.now().isBefore(actualDate);
    }

    @PrePersist
    @PreUpdate
    private void generateDayTag() {
        // Auto-generate dayTag if not set
        if (this.dayTag == null || this.dayTag.isEmpty()) {
            if (this.dayNumber != null) {
                this.dayTag = "Day " + this.dayNumber;
            }
        }
    }
}
