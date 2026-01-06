package com.itineraryledger.kabengosafaris.Accommodation.DTOs.AccommodationRoomTypeDTOs;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * AccommodationRoomTypeDTO - Response DTO for accommodation room types
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AccommodationRoomTypeDTO {

    private String id; // Obfuscated ID

    private String accommodationId; // Obfuscated ID

    private String accommodationName;

    private String name;

    private String bedConfiguration;

    private Integer maxOccupancy;

    private Integer minOccupancy;

    private String description;

    private Boolean isActive;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
