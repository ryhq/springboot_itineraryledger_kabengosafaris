package com.itineraryledger.kabengosafaris.Activity.DTOs.ActivityImageDTOs;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for reordering activity images.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReorderActivityImagesDTO {

    @NotBlank(message = "Activity ID is required")
    private String activityId;

    @NotEmpty(message = "Image order list is required")
    private List<String> imageOrder;
}
