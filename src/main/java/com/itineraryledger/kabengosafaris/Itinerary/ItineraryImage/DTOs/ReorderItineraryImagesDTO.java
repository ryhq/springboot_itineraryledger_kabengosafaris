package com.itineraryledger.kabengosafaris.Itinerary.ItineraryImage.DTOs;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for reordering itinerary images within an itinerary.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReorderItineraryImagesDTO {

    @NotBlank(message = "Itinerary ID is required")
    private String itineraryId;

    @NotEmpty(message = "Image order list is required")
    @Valid
    private List<ImageOrderItem> imageOrder;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ImageOrderItem {
        @NotBlank(message = "Image ID is required")
        private String imageId;

        /**
         * Optional: expected display order for validation.
         * If provided and doesn't match position in list, returns error.
         */
        private Integer expectedDisplayOrder;
    }
}
