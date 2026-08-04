package com.itineraryledger.kabengosafaris.ParkActivity.DTOs.ParkActivityImageDTOs;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.itineraryledger.kabengosafaris.ParkActivity.Entities.ParkActivityImage.ImageType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for ParkActivityImage responses.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ParkActivityImageDTO {

    private String id;

    // Park-Activity relationship identifiers
    private String parkId;
    private String parkName;
    private String activityId;
    private String activityName;

    private ImageType imageType;
    private String imageTypeDisplayName;
    private String imageTypeDescription;

    /**
     * Full URL to the image using obfuscated ID
     */
    private String imageUrl;

    /**
     * Full URL to the image using filename
     */
    private String fileImageUrl;

    /**
     * Stored filename only (for reference)
     */
    private String fileName;

    /**
     * Original filename uploaded by user
     */
    private String originalFileName;

    private String altText;
    private String caption;
    private String description;

    private Boolean isPrimary;
    private Boolean isActive;

    private Boolean isWebActive;
    private Integer displayOrder;

    private Long fileSize;
    private String fileSizeFormatted;
    private String mimeType;
    private Integer width;
    private Integer height;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
