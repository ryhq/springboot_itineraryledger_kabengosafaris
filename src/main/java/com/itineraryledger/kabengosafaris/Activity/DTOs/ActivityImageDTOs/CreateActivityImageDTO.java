package com.itineraryledger.kabengosafaris.Activity.DTOs.ActivityImageDTOs;

import org.springframework.web.multipart.MultipartFile;

import com.itineraryledger.kabengosafaris.Activity.Entities.ActivityImage.ImageType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for creating a new ActivityImage.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateActivityImageDTO {

    private String activityId;

    private MultipartFile image;

    private ImageType imageType;

    private String altText;

    private String caption;

    private String description;
}
