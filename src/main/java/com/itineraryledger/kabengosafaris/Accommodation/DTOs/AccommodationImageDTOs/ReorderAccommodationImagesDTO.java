package com.itineraryledger.kabengosafaris.Accommodation.DTOs.AccommodationImageDTOs;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * ReorderAccommodationImagesDTO - Data Transfer Object for reordering accommodation images
 *
 * Used when the UI performs drag-and-drop reordering of images within an accommodation.
 * The order list should contain ALL image IDs in the new desired order.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReorderAccommodationImagesDTO {

    /**
     * The accommodation ID (obfuscated) that the images belong to
     */
    @NotNull(message = "Accommodation ID is required")
    private String accommodationId;

    /**
     * List of image reorder items representing the new order.
     * The position in the list determines the new displayOrder (1-indexed).
     * First item becomes displayOrder 1, second becomes displayOrder 2, etc.
     */
    @NotNull(message = "Image order list is required")
    @NotEmpty(message = "Image order list cannot be empty")
    @Valid
    private List<ImageOrderItem> imageOrder;

    /**
     * Inner class representing a single image's position in the new order
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ImageOrderItem {

        @NotNull(message = "Image ID is required")
        private String imageId; // Obfuscated accommodation image ID

        /**
         * Optional: The expected new display order (for validation/confirmation)
         * If provided, it will be validated against the position in the list
         */
        private Integer expectedDisplayOrder;
    }
}
