package com.itineraryledger.kabengosafaris.Itinerary.ItineraryPax.DTOs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * ItineraryPaxDTO - Data Transfer Object for ItineraryPax entity
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ItineraryPaxDTO {
    private String id;
    private String itineraryId;
    private String nationCategoryId;
    private String nationCategoryName;
    private String ageCategoryId;
    private String ageCategoryName;
    private Integer count;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
