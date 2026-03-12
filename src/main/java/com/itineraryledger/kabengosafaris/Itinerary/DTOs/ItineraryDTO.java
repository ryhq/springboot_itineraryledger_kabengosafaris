package com.itineraryledger.kabengosafaris.Itinerary.DTOs;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.itineraryledger.kabengosafaris.Itinerary.Entity.BudgetCategory;
import com.itineraryledger.kabengosafaris.Itinerary.Entity.TripType;
import com.itineraryledger.kabengosafaris.Itinerary.Entity.Itinerary.ItineraryStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.DTOs.ItineraryCostSummaryDTO;

import com.itineraryledger.kabengosafaris.Public.Annotations.Translatable;

import java.time.LocalDateTime;
import java.util.List;

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
    @Translatable private String name;
    private String code;
    private ItineraryStatus status;
    @Translatable private String statusDisplayName;
    private TripType tripType;
    @Translatable private String tripTypeDisplayName;
    @Translatable private String tripTypeDescription;
    private BudgetCategory budgetCategory;
    @Translatable private String budgetCategoryDisplayName;
    @Translatable private String budgetCategoryDescription;
    private Integer budgetCategoryTier;
    private Integer totalDays;
    private Integer totalNights;
    private Boolean isDayTrip;
    private Integer carCount;
    @Translatable private String description;
    @Translatable private String highlights;
    @Translatable private String startLocation;
    @Translatable private String endLocation;
    private Boolean isActive;
    private Integer totalPaxCount;
    private Integer totalDaysCount;
    private String primaryImageUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<ItineraryCostSummaryDTO> costSummary;
}
