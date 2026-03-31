package com.itineraryledger.kabengosafaris.Safari.SafariVehicle.DTOs;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateSafariVehicleDTO {

    @NotBlank(message = "Vehicle ID is required")
    private String vehicleId;

    private LocalDate startDate;
    private LocalDate endDate;
    private String driverName;
    private String driverPhone;
    private String assignmentNotes;
}
