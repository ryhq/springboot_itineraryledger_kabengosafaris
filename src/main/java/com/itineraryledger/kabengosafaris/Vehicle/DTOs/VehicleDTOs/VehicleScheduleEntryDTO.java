package com.itineraryledger.kabengosafaris.Vehicle.DTOs.VehicleDTOs;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.LocalDate;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VehicleScheduleEntryDTO {
    private String id;
    private String type;          // "SAFARI" or "HIRE"
    private LocalDate startDate;
    private LocalDate endDate;
    private String description;
    private String status;
    private String referenceId;
    private String referenceName;
    private String driverName;
    private String driverPhone;
}
