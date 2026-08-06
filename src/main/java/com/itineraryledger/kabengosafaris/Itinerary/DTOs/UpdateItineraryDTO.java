package com.itineraryledger.kabengosafaris.Itinerary.DTOs;

import com.itineraryledger.kabengosafaris.Itinerary.Entity.BudgetCategory;
import com.itineraryledger.kabengosafaris.Itinerary.Entity.TripType;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * UpdateItineraryDTO - Data Transfer Object for updating an Itinerary
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateItineraryDTO {

    @Size(min = 3, max = 200, message = "Name must be between 3 and 200 characters")
    private String name;

    /*
     * Enums arrive as Strings so a blank can CLEAR the field; null still means
     * "leave unchanged". Bound as the enum itself, an empty value makes Jackson
     * reject the whole request body, which is how a set value became impossible
     * to unset (see the charging-basis fix).
     */
    private String tripType;
    private String budgetCategory;

    @Min(value = 1, message = "Total days must be at least 1")
    private Integer totalDays;

    @Min(value = 0, message = "Total nights cannot be negative")
    private Integer totalNights;

    @Min(value = 1, message = "Car count must be at least 1")
    private Integer carCount;

    private String description;
    private String highlights;

    private String inclusions;

    private String exclusions;
    private String startLocation;
    private String endLocation;
    private Boolean isActive;
}
