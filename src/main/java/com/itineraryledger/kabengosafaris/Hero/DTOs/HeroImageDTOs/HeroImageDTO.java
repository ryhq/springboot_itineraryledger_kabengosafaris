package com.itineraryledger.kabengosafaris.Hero.DTOs.HeroImageDTOs;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for HeroImage entity.
 * Contains complete image information including obfuscated IDs, file details,
 * display configuration, metadata, and audit timestamps.
 * Constructs the full image URL from the stored filename and configured base URL.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class HeroImageDTO {

    /**
     * Obfuscated hero image ID
     */
    private String id;

    /**
     * Obfuscated ID of the hero this image belongs to
     */
    private String heroId;

    /**
     * Full URL to the image using obfuscated ID
     * Example: "http://localhost:4450/api/hero-images/{id}/file"
     */
    private String imageUrl;

    /**
     * Full URL to the image using filename
     * Example: "http://localhost:4450/api/hero-images/file/{fileName}"
     */
    private String fileImageUrl;

    /**
     * Stored file name on the server (for reference)
     */
    private String fileName;

    /**
     * Original file name as uploaded by the user
     */
    private String originalFileName;

    /**
     * Alternative text for accessibility
     */
    private String altText;

    /**
     * Caption displayed with the image
     */
    private String caption;

    /**
     * Detailed description of the image
     */
    private String description;

    /**
     * Whether this is the primary/featured image for the hero
     */
    private Boolean isPrimary;

    /**
     * Whether this image is active and should be displayed
     */
    private Boolean isActive;

    /**
     * Order in which this image should be displayed
     */
    private Integer displayOrder;

    /**
     * File size in bytes
     */
    private Long fileSize;

    /**
     * Formatted file size (e.g., "1.5 MB", "250 KB")
     */
    private String fileSizeFormatted;

    /**
     * MIME type of the image (e.g., image/jpeg, image/png)
     */
    private String mimeType;

    /**
     * Image width in pixels
     */
    private Integer width;

    /**
     * Image height in pixels
     */
    private Integer height;

    /**
     * Timestamp when this image was created
     */
    private LocalDateTime createdAt;

    /**
     * Timestamp when this image was last updated
     */
    private LocalDateTime updatedAt;
}
