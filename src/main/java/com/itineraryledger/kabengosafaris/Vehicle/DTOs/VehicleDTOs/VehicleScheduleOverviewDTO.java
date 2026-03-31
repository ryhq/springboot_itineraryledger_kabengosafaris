package com.itineraryledger.kabengosafaris.Vehicle.DTOs.VehicleDTOs;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.util.List;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VehicleScheduleOverviewDTO {
    private String vehicleId;
    private String vehicleName;
    private String registrationNumber;
    private String vehicleType;
    private Integer capacity;
    private Boolean isActive;
    private List<VehicleScheduleEntryDTO> entries;
    private int totalAssignments;
}
