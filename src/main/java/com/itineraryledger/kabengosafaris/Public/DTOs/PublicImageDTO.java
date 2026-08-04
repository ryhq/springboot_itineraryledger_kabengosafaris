package com.itineraryledger.kabengosafaris.Public.DTOs;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.itineraryledger.kabengosafaris.Public.Annotations.Translatable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Public-safe Image DTO — shared across Park, Activity, Accommodation, and Hero images.
 * Excludes: fileName, originalFileName, isPrimary, isActive, displayOrder,
 * fileSize, mimeType, width, height, createdAt, updatedAt, parent entity IDs.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PublicImageDTO {

    private String imageUrl;
    @Translatable private String altText;
    @Translatable private String caption;
    private String imageType;

    /**
     * Set only on park-activity photos: the activity a park's gallery is showing,
     * or the park an activity's gallery is showing. Lets the site caption or group
     * them; null for a record's own images.
     */
    private String activityName;
    private String parkName;
}
