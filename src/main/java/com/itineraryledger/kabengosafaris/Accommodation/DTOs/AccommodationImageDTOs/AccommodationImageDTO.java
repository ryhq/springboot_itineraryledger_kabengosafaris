package com.itineraryledger.kabengosafaris.Accommodation.DTOs.AccommodationImageDTOs;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.itineraryledger.kabengosafaris.Accommodation.Entities.AccommodationImage.ImageType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for AccommodationImage responses.
 * Constructs the full image URL from the stored filename and configured base URL.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AccommodationImageDTO {

    private String id;
    private String accommodationId;
    private String accommodationName;

    /**
     * Full URL to the image (constructed from base URL + filename)
     * Example: "http://localhost/kabengosafaris/accommodation-images/a1b2c3d4.jpg"
     */
    private String imageUrl;

    /**
     * Stored filename only (for reference)
     */
    private String fileName;

    /**
     * Original filename uploaded by user
     */
    private String originalFileName;

    private ImageType imageType;
    private String imageTypeDisplayName;
    private String imageTypeDescription;

    private String altText;
    private String caption;
    private String description;

    private Boolean isPrimary;
    private Boolean isActive;
    private Integer displayOrder;

    private Long fileSize;
    private String fileSizeFormatted;
    private String mimeType;
    private Integer width;
    private Integer height;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
