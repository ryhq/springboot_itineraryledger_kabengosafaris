package com.itineraryledger.kabengosafaris.Accommodation.DTOs.AccommodationRoomTypeDTOs;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * UpdateAccommodationRoomTypeDTO - DTO for updating accommodation room types
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UpdateAccommodationRoomTypeDTO {

    private String name;

    private String bedConfiguration;

    private Integer maxOccupancy;

    private Integer minOccupancy;

    private String description;

    private Boolean isActive;
}
