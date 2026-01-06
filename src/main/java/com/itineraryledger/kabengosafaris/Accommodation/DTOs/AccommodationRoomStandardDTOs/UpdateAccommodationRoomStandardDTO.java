package com.itineraryledger.kabengosafaris.Accommodation.DTOs.AccommodationRoomStandardDTOs;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * UpdateAccommodationRoomStandardDTO - DTO for updating accommodation room standards
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UpdateAccommodationRoomStandardDTO {

    private String name;

    private String description;

    private Integer maxOccupancy;

    private String amenities;

    private String viewType;

    private String floorLevel;

    private Boolean isActive;
}
