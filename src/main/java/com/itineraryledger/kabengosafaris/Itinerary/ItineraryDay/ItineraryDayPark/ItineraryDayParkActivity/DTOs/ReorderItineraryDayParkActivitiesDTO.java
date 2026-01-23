package com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.ItineraryDayParkActivity.DTOs;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * ReorderItineraryDayParkActivitiesDTO - Data Transfer Object for reordering activities within a park visit
 *
 * Used for drag-and-drop reordering of activities from the UI.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReorderItineraryDayParkActivitiesDTO {

    @NotNull(message = "Activity order is required")
    @NotEmpty(message = "Activity order cannot be empty")
    @Valid
    private List<ActivityOrderItem> activityOrder;

    /**
     * Represents a single activity in the new order
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActivityOrderItem {
        @NotNull(message = "Activity ID is required")
        private String activityId;

        /**
         * Optional: Expected sort order for validation
         * If provided, validates the client's expected position matches the actual new position
         */
        private Integer expectedSortOrder;
    }
}
