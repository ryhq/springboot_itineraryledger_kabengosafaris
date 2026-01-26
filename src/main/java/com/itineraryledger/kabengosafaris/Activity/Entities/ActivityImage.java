package com.itineraryledger.kabengosafaris.Activity.Entities;

import com.itineraryledger.kabengosafaris.Activity.Activity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * ActivityImage Entity - Manages images for activities
 *
 * Stores various image files related to activities:
 * - Action shots and photography
 * - Equipment and gear images
 * - Location/venue images
 * - Safety demonstration images
 * - Promotional materials
 */
@Entity
@Table(name = "activity_images", indexes = {
    @Index(name = "idx_activity_image_activity_id", columnList = "activity_id"),
    @Index(name = "idx_activity_image_type", columnList = "image_type"),
    @Index(name = "idx_activity_image_is_primary", columnList = "is_primary"),
    @Index(name = "idx_activity_image_is_active", columnList = "is_active"),
    @Index(name = "idx_activity_image_display_order", columnList = "display_order")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActivityImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "activity_id", nullable = false)
    private Activity activity;

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
     * Activity-specific image type enumeration
     */
    public enum ImageType {
        ACTION("Action Shot", "Action and activity photography"),
        EQUIPMENT("Equipment", "Equipment and gear images"),
        LOCATION("Location", "Activity location or venue"),
        SAFETY("Safety", "Safety demonstration or instructions"),
        GROUP("Group Shot", "Group activity photos"),
        GUIDE("Guide", "Guide or instructor images"),
        WILDLIFE("Wildlife", "Wildlife encountered during activity"),
        SCENIC("Scenic", "Scenic views during activity"),
        AERIAL("Aerial", "Aerial or drone photography"),
        UNDERWATER("Underwater", "Underwater photography"),
        NIGHT("Night Shot", "Night activity photography"),
        SUNRISE("Sunrise", "Sunrise activity photos"),
        SUNSET("Sunset", "Sunset activity photos"),
        PROMOTIONAL("Promotional", "Marketing and promotional images"),
        THUMBNAIL("Thumbnail", "Thumbnail or preview image"),
        BANNER("Banner", "Banner or header image"),
        GALLERY("Gallery", "General gallery image"),
        BEFORE_AFTER("Before/After", "Before and after comparison"),
        INSTRUCTIONAL("Instructional", "Step-by-step instructional images"),
        CERTIFICATE("Certificate", "Certificates or badges"),
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
