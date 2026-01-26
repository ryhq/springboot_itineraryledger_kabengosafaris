package com.itineraryledger.kabengosafaris.ParkActivity.DTOs.ParkActivityImageDTOs;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for reordering park activity images.
 * Both parkId and activityId are required to identify the park-activity relationship.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReorderParkActivityImagesDTO {

    @NotBlank(message = "Park ID is required")
    private String parkId;

    @NotBlank(message = "Activity ID is required")
    private String activityId;

    @NotEmpty(message = "Image order list is required")
    private List<String> imageOrder;
}
