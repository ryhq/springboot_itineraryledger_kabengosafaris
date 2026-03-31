package com.itineraryledger.kabengosafaris.Vehicle.DTOs.VehicleDTOs;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.itineraryledger.kabengosafaris.Vehicle.Enums.VehicleType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VehicleListItemDTO {

    private String id;
    private String name;
    private String registrationNumber;
    private VehicleType type;
    private String typeDisplayName;
    private Integer capacity;
    private Boolean isActive;
}
