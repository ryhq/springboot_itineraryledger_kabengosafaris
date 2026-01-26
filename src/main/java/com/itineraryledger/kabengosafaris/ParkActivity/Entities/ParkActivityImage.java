package com.itineraryledger.kabengosafaris.ParkActivity.Entities;

import com.itineraryledger.kabengosafaris.ParkActivity.ParkActivity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * ParkActivityImage Entity - Manages images for park-activity relationships
 *
 * Stores various image files related to activities within specific parks:
 * - Action shots specific to this park's activity
 * - Location-specific equipment images
 * - Venue-specific photography
 * - Park-activity promotional materials
 *
 * The ParkActivity relationship MUST exist for an image to be created.
 */
@Entity
@Table(name = "park_activity_images", indexes = {
    @Index(name = "idx_park_activity_image_park_id", columnList = "park_id"),
    @Index(name = "idx_park_activity_image_activity_id", columnList = "activity_id"),
    @Index(name = "idx_park_activity_image_type", columnList = "image_type"),
    @Index(name = "idx_park_activity_image_is_primary", columnList = "is_primary"),
    @Index(name = "idx_park_activity_image_is_active", columnList = "is_active"),
    @Index(name = "idx_park_activity_image_display_order", columnList = "display_order")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParkActivityImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Reference to the ParkActivity relationship.
     * Both park_id and activity_id are required as a composite foreign key.
     * The ParkActivity must exist for this image to exist.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumns({
        @JoinColumn(name = "park_id", referencedColumnName = "park_id", nullable = false),
        @JoinColumn(name = "activity_id", referencedColumnName = "activity_id", nullable = false)
    })
    private ParkActivity parkActivity;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "original_file_name", length = 255)
    private String originalFileName;

    @Enumerated(EnumType.STRING)
    @Column(name = "image_type", nullable = false, length = 50)
    @Builder.Default
    private ImageType imageType = ImageType.OTHER;

    @Column(name = "alt_text", length = 500)
    private String altText;

    @Column(name = "caption", length = 500)
    private String caption;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Builder.Default
    @Column(name = "is_primary", nullable = false)
    private Boolean isPrimary = false;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Builder.Default
    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "mime_type", length = 100)
    private String mimeType;

    @Column(name = "width")
    private Integer width;

    @Column(name = "height")
    private Integer height;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Park-Activity specific image type enumeration
     * Combines activity imagery with park-specific context
     */
    public enum ImageType {
        ACTION("Action Shot", "Action photography of activity at this park"),
        EQUIPMENT("Equipment", "Equipment and gear specific to this park"),
        LOCATION("Location", "Activity location within this park"),
        SAFETY("Safety", "Safety demonstrations at this park"),
        GROUP("Group Shot", "Group photos from this park activity"),
        GUIDE("Guide", "Guide or instructor at this park"),
        WILDLIFE("Wildlife", "Wildlife seen during activity at this park"),
        SCENIC("Scenic", "Scenic views during activity at this park"),
        AERIAL("Aerial", "Aerial photography of activity at this park"),
        UNDERWATER("Underwater", "Underwater photography at this park"),
        NIGHT("Night Shot", "Night activity photography at this park"),
        SUNRISE("Sunrise", "Sunrise activity photos at this park"),
        SUNSET("Sunset", "Sunset activity photos at this park"),
        PROMOTIONAL("Promotional", "Marketing images for this park activity"),
        THUMBNAIL("Thumbnail", "Thumbnail or preview image"),
        BANNER("Banner", "Banner or header image"),
        GALLERY("Gallery", "General gallery image"),
        BEFORE_AFTER("Before/After", "Before and after comparison"),
        INSTRUCTIONAL("Instructional", "Step-by-step instructional images"),
        MEETING_POINT("Meeting Point", "Meeting point or gathering area"),
        FACILITY("Facility", "Facility or amenity images"),
        OTHER("Other", "Other image type");

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
