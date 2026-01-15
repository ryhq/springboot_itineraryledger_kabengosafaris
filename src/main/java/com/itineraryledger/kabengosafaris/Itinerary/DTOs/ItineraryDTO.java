package com.itineraryledger.kabengosafaris.Itinerary.DTOs;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.itineraryledger.kabengosafaris.Itinerary.Entity.BudgetCategory;
import com.itineraryledger.kabengosafaris.Itinerary.Entity.TripType;
import com.itineraryledger.kabengosafaris.Itinerary.Entity.Itinerary.ItineraryStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * ItineraryDTO - Data Transfer Object for Itinerary entity
 * Contains obfuscated ID for secure data transfer
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ItineraryDTO {
    private String id;
    private String name;
    private String code;
    private ItineraryStatus status;
    private String statusDisplayName;
    private TripType tripType;
    private String tripTypeDisplayName;
    private String tripTypeDescription;
    private BudgetCategory budgetCategory;
    private String budgetCategoryDisplayName;
    private String budgetCategoryDescription;
    private Integer budgetCategoryTier;
    private Integer totalDays;
    private Integer totalNights;
    private Boolean isDayTrip;
    private Integer carCount;
    private String description;
    private String highlights;
    private String startLocation;
    private String endLocation;
    private Boolean isActive;
    private Integer totalPaxCount;
    private Integer totalDaysCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
