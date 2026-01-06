package com.itineraryledger.kabengosafaris.Accommodation.DTOs.AccommodationRoomStandardDTOs;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * AccommodationRoomStandardDTO - Response DTO for accommodation room standards
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AccommodationRoomStandardDTO {

    private String id; // Obfuscated ID

    private String accommodationId; // Obfuscated ID

    private String accommodationName;

    private String name;

    private String description;

    private Integer maxOccupancy;

    private String amenities;

    private String viewType;

    private String floorLevel;

    private Boolean isActive;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
