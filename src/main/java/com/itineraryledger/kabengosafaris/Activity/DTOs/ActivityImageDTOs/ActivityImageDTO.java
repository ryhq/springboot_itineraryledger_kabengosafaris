package com.itineraryledger.kabengosafaris.Activity.DTOs.ActivityImageDTOs;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.itineraryledger.kabengosafaris.Activity.Entities.ActivityImage.ImageType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for ActivityImage responses.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ActivityImageDTO {

    private String id;
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
    private Integer displayOrder;

    private Long fileSize;
    private String fileSizeFormatted;
    private String mimeType;
    private Integer width;
    private Integer height;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
