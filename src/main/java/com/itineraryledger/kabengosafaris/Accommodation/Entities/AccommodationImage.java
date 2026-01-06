package com.itineraryledger.kabengosafaris.Accommodation.Entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * AccommodationImage Entity - Manages images for accommodations
 *
 * Supports multiple images per accommodation with different types (exterior, rooms, dining, etc.)
 */
@Entity
@Table(name = "accommodation_images", indexes = {
    @Index(name = "idx_accommodation_image_accommodation_id", columnList = "accommodation_id"),
    @Index(name = "idx_accommodation_image_type", columnList = "image_type"),
    @Index(name = "idx_accommodation_image_is_primary", columnList = "is_primary"),
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccommodationImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "accommodation_id", nullable = false)
    private Accommodation accommodation;

    @Column(name = "image_url", nullable = false, length = 1000)
    private String imageUrl; // URL or path to the image

    @Enumerated(EnumType.STRING)
    @Column(name = "image_type", nullable = false, length = 50)
    private ImageType imageType;

    @Builder.Default
    @Column(name = "is_primary", nullable = false)
    private Boolean isPrimary = false; // Primary/featured image

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Lob
    @Column(name = "description", columnDefinition = "TEXT")
    private String description; // Detailed description

    @Column(name = "file_size")
    private Long fileSize; // File size in bytes

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Image types for accommodation
     */
    public enum ImageType {
        EXTERIOR("Exterior", "Exterior/facade of the accommodation"),
        INTERIOR("Interior", "Interior common areas"),
        ROOM("Room", "Guest rooms"),
        BATHROOM("Bathroom", "Bathrooms"),
        DINING("Dining", "Restaurant and dining areas"),
        POOL("Pool", "Swimming pool"),
        SPA("Spa", "Spa and wellness facilities"),
        GYM("Gym", "Fitness center"),
        CONFERENCE("Conference", "Conference and meeting rooms"),
        GARDEN("Garden", "Gardens and outdoor spaces"),
        VIEW("View", "Views from the property"),
        AMENITY("Amenity", "Amenities and facilities"),
        ACTIVITY("Activity", "Activities available"),
        NEARBY("Nearby Attraction", "Nearby attractions"),
        FOOD("Food", "Food and beverages"),
        STAFF("Staff", "Staff and service"),
        LOGO("Logo", "Accommodation logo"),
        OTHER("Other", "Other images");

        private final String displayName;
        private final String description;

        ImageType(String displayName, String description) {
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
