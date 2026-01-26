package com.itineraryledger.kabengosafaris.Park.DTOs.ParkImageDTOs;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.itineraryledger.kabengosafaris.Park.Entities.ParkImage.ImageType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for ParkImage responses.
 * Constructs the full image URL from the stored filename and configured base URL.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ParkImageDTO {

    private String id;
    private String parkId;
    private String parkName;

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
