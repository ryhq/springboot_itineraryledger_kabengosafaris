package com.itineraryledger.kabengosafaris.Quote.QuoteDay.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.itineraryledger.kabengosafaris.Quote.Entity.Quote;
import com.itineraryledger.kabengosafaris.Quote.QuoteDay.QuoteDayAccommodation.Entity.QuoteDayAccommodation;
import com.itineraryledger.kabengosafaris.Quote.QuoteDay.QuoteDayActivity.Entity.QuoteDayActivity;
import com.itineraryledger.kabengosafaris.Quote.QuoteDay.QuoteDayPark.Entity.QuoteDayPark;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * QuoteDay - per-customer working copy of an itinerary day.
 *
 * Snapshotted from ItineraryDay when a Quote is generated from an Itinerary.
 * Operational fields (actualDate, weatherNotes, actualStartTime, actualEndTime,
 * driverNotes) live on SafariDay only — Quote is a negotiation document, not a
 * live booking.
 */
@Entity
@Table(name = "quote_days",
    indexes = {
        @Index(name = "idx_quote_day_quote_id", columnList = "quote_id"),
        @Index(name = "idx_quote_day_number", columnList = "day_number")
    },
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_quote_day_number",
            columnNames = {"quote_id", "day_number"}
        )
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuoteDay {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quote_id", nullable = false)
    @JsonIgnore
    private Quote quote;

    @Column(name = "day_number", nullable = false)
    private Integer dayNumber;

    @Column(name = "day_tag", length = 50)
    private String dayTag;

    @Column(nullable = false, length = 200)
    private String title;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String description;

    @Lob
    @Column(name = "morning_activities", columnDefinition = "TEXT")
    private String morningActivities;

    @Lob
    @Column(name = "afternoon_activities", columnDefinition = "TEXT")
    private String afternoonActivities;

    @Lob
    @Column(name = "evening_activities", columnDefinition = "TEXT")
    private String eveningActivities;

    @Lob
    @Column(name = "wildlife_highlights", columnDefinition = "TEXT")
    private String wildlifeHighlights;

    @Lob
    @Column(name = "scenic_highlights", columnDefinition = "TEXT")
    private String scenicHighlights;

    @Lob
    @Column(name = "special_notes", columnDefinition = "TEXT")
    private String specialNotes;

    @Column(name = "start_location", length = 200)
    private String startLocation;

    @Column(name = "end_location", length = 200)
    private String endLocation;

    @Column(name = "distance_km")
    private Integer distanceKm;

    @Builder.Default
    @Column(name = "is_overnight", nullable = false)
    private Boolean isOvernight = true;

    @Column(name = "meals_included", length = 100)
    private String mealsIncluded;

    @Lob
    @Column(name = "internal_notes", columnDefinition = "TEXT")
    private String internalNotes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "quoteDay", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @OrderBy("sortOrder ASC")
    private List<QuoteDayActivity> activities = new ArrayList<>();

    @OneToMany(mappedBy = "quoteDay", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @OrderBy("sortOrder ASC")
    private List<QuoteDayPark> parks = new ArrayList<>();

    @OneToMany(mappedBy = "quoteDay", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<QuoteDayAccommodation> accommodations = new ArrayList<>();

    public void addActivity(QuoteDayActivity activity) {
        activities.add(activity);
        activity.setQuoteDay(this);
    }

    public void removeActivity(QuoteDayActivity activity) {
        activities.remove(activity);
        activity.setQuoteDay(null);
    }

    public void addPark(QuoteDayPark park) {
        parks.add(park);
        park.setQuoteDay(this);
    }

    public void removePark(QuoteDayPark park) {
        parks.remove(park);
        park.setQuoteDay(null);
    }

    public void addAccommodation(QuoteDayAccommodation accommodation) {
        accommodations.add(accommodation);
        accommodation.setQuoteDay(this);
    }

    public void removeAccommodation(QuoteDayAccommodation accommodation) {
        accommodations.remove(accommodation);
        accommodation.setQuoteDay(null);
    }

    @Transient
    public boolean hasParks() {
        return parks != null && !parks.isEmpty();
    }

    @Transient
    public boolean hasActivities() {
        return activities != null && !activities.isEmpty();
    }

    @Transient
    public boolean hasAccommodation() {
        return accommodations != null && !accommodations.isEmpty();
    }

    @PrePersist
    @PreUpdate
    private void generateDayTag() {
        if (this.dayTag == null || this.dayTag.isEmpty()) {
            if (this.dayNumber != null) {
                this.dayTag = "Day " + this.dayNumber;
            }
        }
    }
}
