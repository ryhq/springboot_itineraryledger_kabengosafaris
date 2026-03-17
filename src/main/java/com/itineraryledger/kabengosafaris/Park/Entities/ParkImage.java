package com.itineraryledger.kabengosafaris.Park.Entities;

import com.itineraryledger.kabengosafaris.Park.Park;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * ParkImage Entity - Manages images for parks and reserves
 *
 * Stores various image types related to parks including:
 * - Landscape and scenic views
 * - Wildlife photos
 * - Entrance gates
 * - Maps and illustrations
 * - Accommodation facilities within parks
 */
@Entity
@Table(name = "park_images", indexes = {
    @Index(name = "idx_park_image_park_id", columnList = "park_id"),
    @Index(name = "idx_park_image_type", columnList = "image_type"),
    @Index(name = "idx_park_image_is_active", columnList = "is_active"),
    @Index(name = "idx_park_image_web_active", columnList = "is_web_active"),
    @Index(name = "idx_park_image_is_primary", columnList = "is_primary"),
    @Index(name = "idx_park_image_display_order", columnList = "display_order")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParkImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "park_id", nullable = false)
    private Park park;

    @Column(name = "file_name", nullable = false, length = 500)
    private String fileName;

    @Column(name = "original_file_name", length = 255)
    private String originalFileName;

    @Enumerated(EnumType.STRING)
    @Column(name = "image_type", nullable = false, length = 50)
    private ImageType imageType;

    @Column(name = "alt_text", length = 255)
    private String altText;

    @Column(length = 255)
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
    @Column(name = "is_web_active", nullable = false)
    private Boolean isWebActive = true;

    @Builder.Default
    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "mime_type", length = 100)
    private String mimeType;

    @Column
    private Integer width;

    @Column
    private Integer height;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Park-specific image type enumeration
     */
    public enum ImageType {
        LANDSCAPE("Landscape", "Scenic landscape views of the park"),
        WILDLIFE("Wildlife", "Wildlife and animal photos"),
        BIRD("Bird", "Bird watching and avian species"),
        ENTRANCE("Entrance", "Park entrance gate and signage"),
        VIEWPOINT("Viewpoint", "Famous viewpoints and lookout spots"),
        VEGETATION("Vegetation", "Flora, trees, and plant life"),
        WATER_BODY("Water Body", "Rivers, lakes, waterfalls, and water features"),
        SUNSET_SUNRISE("Sunset/Sunrise", "Sunrise and sunset views"),
        AERIAL("Aerial", "Aerial and drone photography"),
        MAP("Map", "Park maps and illustrated guides"),
        TRAIL("Trail", "Hiking trails and paths"),
        CAMPSITE("Campsite", "Camping areas and facilities"),
        LODGE("Lodge", "Lodges and accommodation within park"),
        VISITOR_CENTER("Visitor Center", "Visitor centers and information points"),
        VEHICLE("Vehicle", "Safari vehicles and transportation"),
        ACTIVITY("Activity", "Activities being performed in the park"),
        HISTORICAL("Historical", "Historical sites and monuments"),
        MIGRATION("Migration", "Great migration and animal movements"),
        NIGHT("Night", "Night photography and nocturnal scenes"),
        LOGO("Logo", "Park logo or emblem"),
        OTHER("Other", "Other park images");

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
