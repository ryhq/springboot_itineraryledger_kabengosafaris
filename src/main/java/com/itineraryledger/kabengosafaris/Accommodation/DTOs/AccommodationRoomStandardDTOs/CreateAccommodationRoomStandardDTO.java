package com.itineraryledger.kabengosafaris.Accommodation.DTOs.AccommodationRoomStandardDTOs;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * CreateAccommodationRoomStandardDTO - DTO for creating accommodation room standards
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CreateAccommodationRoomStandardDTO {

    @NotBlank(message = "Accommodation ID is required")
    private String accommodationId; // Obfuscated ID

    @NotBlank(message = "Room standard name is required")
    private String name;

    private String description;

    private Integer maxOccupancy;

    private String amenities;

    private String viewType;

    private String floorLevel;

    private Boolean isActive;
}
