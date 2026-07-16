package com.itineraryledger.kabengosafaris.Itinerary.ItineraryImage.Entity;

import com.itineraryledger.kabengosafaris.Itinerary.Entity.Itinerary;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * ItineraryImage Entity - Manages hero + gallery images for a safari itinerary.
 *
 * A safari card uses the primary image (isPrimary) of its itinerary as the hero.
 * When no image is uploaded, the public layer falls back to a deterministic pick
 * from the trip's linked parks/activities (see PublicImageResolver).
 */
@Entity
@Table(name = "itinerary_images", indexes = {
    @Index(name = "idx_itinerary_image_itinerary_id", columnList = "itinerary_id"),
    @Index(name = "idx_itinerary_image_type", columnList = "image_type"),
    @Index(name = "idx_itinerary_image_is_active", columnList = "is_active"),
    @Index(name = "idx_itinerary_image_web_active", columnList = "is_web_active"),
    @Index(name = "idx_itinerary_image_is_primary", columnList = "is_primary"),
    @Index(name = "idx_itinerary_image_display_order", columnList = "display_order")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItineraryImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "itinerary_id", nullable = false)
    private Itinerary itinerary;

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
     * Itinerary-specific image type enumeration
     */
    public enum ImageType {
        HERO("Hero", "Main hero/cover image for the safari"),
        GALLERY("Gallery", "General gallery image"),
        LANDSCAPE("Landscape", "Scenic landscape views"),
        WILDLIFE("Wildlife", "Wildlife and animal photos"),
        LODGE("Lodge", "Lodges and accommodation on the trip"),
        ACTIVITY("Activity", "Activities and experiences on the trip"),
        BEACH("Beach", "Beach and coastal scenes"),
        CULTURE("Culture", "Culture and people"),
        MAP("Map", "Route maps and illustrated guides"),
        OTHER("Other", "Other itinerary images");

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
