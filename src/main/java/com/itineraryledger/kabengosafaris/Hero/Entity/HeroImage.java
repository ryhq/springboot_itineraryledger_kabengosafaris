package com.itineraryledger.kabengosafaris.Hero.Entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * HeroImage Entity - Manages images for hero sections
 *
 * Each hero typically has one background image.
 *
 * NOTE: Only the filename is stored in the database (e.g., "abc123def456.jpg").
 * The full URL is constructed in the DTO using the configured base URL.
 */
@Entity
@Table(name = "hero_images", indexes = {
    @Index(name = "idx_hero_image_hero_id", columnList = "hero_id"),
    @Index(name = "idx_hero_image_is_primary", columnList = "is_primary"),
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HeroImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hero_id", nullable = false)
    private Hero hero;

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
}
