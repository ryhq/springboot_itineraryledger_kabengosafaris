package com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayActivity.DTOs;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * ReorderSafariDayActivitiesDTO - Data Transfer Object for reordering safari day activities
 *
 * Used when the UI performs drag-and-drop reordering of activities within a day.
 * The order list should contain ALL activity IDs in the new desired order.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReorderSafariDayActivitiesDTO {

    /**
     * List of activity reorder items representing the new order.
     * The position in the list determines the new sortOrder (1-indexed).
     * First item becomes sortOrder 1, second becomes sortOrder 2, etc.
     */
    @NotNull(message = "Activity order list is required")
    @NotEmpty(message = "Activity order list cannot be empty")
    @Valid
    private List<ActivityOrderItem> activityOrder;

    /**
     * Inner class representing a single activity's position in the new order
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActivityOrderItem {

        @NotNull(message = "Activity ID is required")
        private String activityId; // Obfuscated safari day activity ID

        /**
         * Optional: The expected new sort order (for validation/confirmation)
         * If provided, it will be validated against the position in the list
         */
        private Integer expectedSortOrder;
    }
}
