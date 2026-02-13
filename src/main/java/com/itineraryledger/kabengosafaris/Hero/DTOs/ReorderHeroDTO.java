package com.itineraryledger.kabengosafaris.Hero.DTOs;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.itineraryledger.kabengosafaris.Hero.Enums.HeroPage;

import java.util.List;

/**
 * ReorderHeroDTO - Data Transfer Object for reordering heroes on a page
 *
 * Used when the UI performs drag-and-drop reordering of heroes within a page.
 * The order list should contain ALL hero IDs for the page in the new desired order.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReorderHeroDTO {

    /**
     * The page where the heroes are displayed
     */
    @NotNull(message = "Page is required")
    private HeroPage page;

    /**
     * List of hero reorder items representing the new order.
     * The position in the list determines the new displayOrder (1-indexed).
     * First item becomes displayOrder 1, second becomes displayOrder 2, etc.
     */
    @NotNull(message = "Hero order list is required")
    @NotEmpty(message = "Hero order list cannot be empty")
    @Valid
    private List<HeroOrderItem> heroOrder;

    /**
     * Inner class representing a single hero's position in the new order
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HeroOrderItem {

        @NotNull(message = "Hero ID is required")
        private String heroId; // Obfuscated hero ID

        /**
         * Optional: The expected new display order (for validation/confirmation)
         * If provided, it will be validated against the position in the list
         */
        private Integer expectedDisplayOrder;
    }
}
