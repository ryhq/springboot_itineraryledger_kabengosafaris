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
    private String inclusions;
    private String exclusions;
    @Translatable private String startLocation;
    @Translatable private String endLocation;
    private Boolean isActive;
    private Integer totalPaxCount;
    private Integer totalDaysCount;
    private String primaryImageUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<ItineraryCostSummaryDTO> costSummary;

    /**
     * The highlights as a list a document can print, however they happen to be stored.
     *
     * <p>Same reasoning as on {@code FullItineraryDTO}: the field is usually a JSON array because
     * that is what the website wants, and a template printing it straight out shows the brackets
     * and quotes to a customer. The safari document reaches the itinerary through this DTO.
     */
    public java.util.List<String> getHighlightsList() {
        if (highlights == null || highlights.isBlank()) return java.util.List.of();
        String trimmed = highlights.trim();
        if (trimmed.startsWith("[")) {
            try {
                java.util.List<String> parsed = new com.fasterxml.jackson.databind.ObjectMapper()
                    .readValue(trimmed, new com.fasterxml.jackson.core.type.TypeReference<java.util.List<String>>() {});
                return parsed.stream().filter(h -> h != null && !h.isBlank()).map(String::trim).toList();
            } catch (Exception e) {
                // unparseable: show it as written rather than showing nothing
            }
        }
        java.util.List<String> lines = java.util.Arrays.stream(trimmed.split("\\r?\\n"))
            .map(String::trim).filter(l -> !l.isEmpty()).toList();
        return lines.isEmpty() ? java.util.List.of(trimmed) : lines;
    }
}
