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

    private TripType tripType;
    private BudgetCategory budgetCategory;

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
