package com.itineraryledger.kabengosafaris.Accommodation.DTOs.AccommodationDTOs;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AccommodationListItemDTO - Lightweight DTO for accommodation dropdown/list selection
 * Used in UI components like select dropdowns when creating emails, phones, etc.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AccommodationListItemDTO {

    private String id; // Encoded ID
    private String name;
    private String location;
    private String region;
    private Boolean isActive;
}
