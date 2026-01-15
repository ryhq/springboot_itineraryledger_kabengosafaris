package com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.itineraryledger.kabengosafaris.Itinerary.Entity.Itinerary;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayAccommodation.Entity.ItineraryDayAccommodation;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayActivity.Entity.ItineraryDayActivity;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.Entity.ItineraryDayPark;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * ItineraryDay Entity - Represents a single day in a safari itinerary
 *
 * Each day can have:
 * - Standalone activities (not tied to a park)
 * - Parks to visit with park-specific activities and tariffs
 * - Accommodation details
 */
@Entity
@Table(name = "itinerary_days",
    indexes = {
        @Index(name = "idx_itinerary_day_itinerary_id", columnList = "itinerary_id"),
        @Index(name = "idx_itinerary_day_number", columnList = "day_number")
    },
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_itinerary_day_number",
            columnNames = {"itinerary_id", "day_number"}
        )
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItineraryDay {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "itinerary_id", nullable = false)
    @JsonIgnore
    private Itinerary itinerary;

    @Column(name = "day_number", nullable = false)
    private Integer dayNumber;

    @Column(name = "day_tag", length = 50)
    private String dayTag; // e.g., "Day 1", "Day 2"

    @Column(nullable = false, length = 200)
    private String title; // e.g., "Arrival in Arusha"

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

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // ========================
    // RELATIONSHIPS
    // ========================

    @OneToMany(mappedBy = "itineraryDay", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @OrderBy("sortOrder ASC")
    private List<ItineraryDayActivity> activities = new ArrayList<>();

    @OneToMany(mappedBy = "itineraryDay", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @OrderBy("sortOrder ASC")
    private List<ItineraryDayPark> parks = new ArrayList<>();

    @OneToMany(mappedBy = "itineraryDay", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ItineraryDayAccommodation> accommodations = new ArrayList<>();

    // ========================
    // HELPER METHODS
    // ========================

    public void addActivity(ItineraryDayActivity activity) {
        activities.add(activity);
        activity.setItineraryDay(this);
    }

    public void removeActivity(ItineraryDayActivity activity) {
        activities.remove(activity);
        activity.setItineraryDay(null);
    }

    public void addPark(ItineraryDayPark park) {
        parks.add(park);
        park.setItineraryDay(this);
    }

    public void removePark(ItineraryDayPark park) {
        parks.remove(park);
        park.setItineraryDay(null);
    }

    public void addAccommodation(ItineraryDayAccommodation accommodation) {
        accommodations.add(accommodation);
        accommodation.setItineraryDay(this);
    }

    public void removeAccommodation(ItineraryDayAccommodation accommodation) {
        accommodations.remove(accommodation);
        accommodation.setItineraryDay(null);
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
