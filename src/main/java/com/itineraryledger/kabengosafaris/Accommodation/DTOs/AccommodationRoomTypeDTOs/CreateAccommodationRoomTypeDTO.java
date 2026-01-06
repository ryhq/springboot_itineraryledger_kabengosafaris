package com.itineraryledger.kabengosafaris.Accommodation.DTOs.AccommodationRoomTypeDTOs;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * CreateAccommodationRoomTypeDTO - DTO for creating accommodation room types
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CreateAccommodationRoomTypeDTO {

    @NotBlank(message = "Accommodation ID is required")
    private String accommodationId; // Obfuscated ID

    @NotBlank(message = "Room type name is required")
    private String name;

    private String bedConfiguration;

    private Integer maxOccupancy;

    private Integer minOccupancy;

    private String description;

    private Boolean isActive;
}
