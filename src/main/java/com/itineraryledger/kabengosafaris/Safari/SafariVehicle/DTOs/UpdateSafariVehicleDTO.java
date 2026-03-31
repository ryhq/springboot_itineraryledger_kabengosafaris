package com.itineraryledger.kabengosafaris.Safari.SafariVehicle.DTOs;

import com.itineraryledger.kabengosafaris.Safari.SafariVehicle.Enums.SafariVehicleStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateSafariVehicleDTO {

    private String vehicleId;
    private LocalDate startDate;
    private LocalDate endDate;
    private String driverName;
    private String driverPhone;
    private String assignmentNotes;
    private SafariVehicleStatus status;
}
