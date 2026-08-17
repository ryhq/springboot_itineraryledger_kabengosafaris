package com.itineraryledger.kabengosafaris.Blog.DTOs.BlogImageDTOs;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * The new order, as a whole list.
 *
 * Position in the list IS the new displayOrder (1-indexed) — the same contract as the
 * accommodation image reorder, which is what the panel already speaks.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReorderBlogImagesDTO {

    @NotNull(message = "Blog ID is required")
    private String blogId;

    @NotNull(message = "Image order list is required")
    @NotEmpty(message = "Image order list cannot be empty")
    @Valid
    private List<ImageOrderItem> imageOrder;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ImageOrderItem {
        @NotNull(message = "Image ID is required")
        private String imageId;
        /** Optional: the position the caller believes this is, for confirmation. */
        private Integer expectedDisplayOrder;
    }
}
