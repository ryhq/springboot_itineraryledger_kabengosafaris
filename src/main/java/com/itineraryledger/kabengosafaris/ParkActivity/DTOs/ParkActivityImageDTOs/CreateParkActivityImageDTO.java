package com.itineraryledger.kabengosafaris.ParkActivity.DTOs.ParkActivityImageDTOs;

import org.springframework.web.multipart.MultipartFile;

import com.itineraryledger.kabengosafaris.ParkActivity.Entities.ParkActivityImage.ImageType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for creating a new ParkActivityImage.
 * Both parkId and activityId are required to identify the park-activity relationship.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateParkActivityImageDTO {

    /**
     * Obfuscated park ID (required)
     */
    private String parkId;

    /**
     * Obfuscated activity ID (required)
     */
    private String activityId;

    /**
     * Image file (required)
     */
    private MultipartFile image;

    /**
     * Type of image (optional, defaults to OTHER)
     */
    private ImageType imageType;

    /**
     * Alt text for accessibility (optional)
     */
    private String altText;

    /**
     * Image caption (optional)
     */
    private String caption;

    /**
     * Detailed description (optional)
     */
    private String description;
}
