package com.itineraryledger.kabengosafaris.Accommodation.Entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * AccommodationImage Entity - Manages images for accommodations
 *
 * Supports multiple images per accommodation with different types (exterior, rooms, dining, etc.)
 *
 * NOTE: Only the filename is stored in the database (e.g., "abc123def456.jpg").
 * The full URL is constructed in the DTO using the configured base URL.
 */
@Entity
@Table(name = "accommodation_images", indexes = {
    @Index(name = "idx_accommodation_image_accommodation_id", columnList = "accommodation_id"),
    @Index(name = "idx_accommodation_image_type", columnList = "image_type"),
    @Index(name = "idx_accommodation_image_is_primary", columnList = "is_primary"),
    @Index(name = "idx_accommodation_image_display_order", columnList = "display_order"),
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

    /**
     * Stored filename only (e.g., "a1b2c3d4e5f6.jpg")
     * The full URL is constructed in the DTO using the configured base URL
     */
    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    /**
     * Original filename uploaded by user (for reference)
     */
    @Column(name = "original_file_name", length = 500)
    private String originalFileName;

    @Enumerated(EnumType.STRING)
    @Column(name = "image_type", nullable = false, length = 50)
    private ImageType imageType;

    /**
     * Alt text for accessibility and SEO
     */
    @Column(name = "alt_text", length = 255)
    private String altText;

    /**
     * Caption/title for the image
     */
    @Column(name = "caption", length = 500)
    private String caption;

    @Builder.Default
    @Column(name = "is_primary", nullable = false)
    private Boolean isPrimary = false; // Primary/featured image

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    /**
     * Display order for sorting images
     */
    @Builder.Default
    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;

    @Lob
    @Column(name = "description", columnDefinition = "TEXT")
    private String description; // Detailed description

    /**
     * File size in bytes
     */
    @Column(name = "file_size")
    private Long fileSize;

    /**
     * MIME type (e.g., "image/jpeg", "image/png")
     */
    @Column(name = "mime_type", length = 100)
    private String mimeType;

    /**
     * Image width in pixels
     */
    @Column(name = "width")
    private Integer width;

    /**
     * Image height in pixels
     */
    @Column(name = "height")
    private Integer height;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

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
