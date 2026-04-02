package com.itineraryledger.kabengosafaris.Safari.SafariVehicle.DTOs;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.itineraryledger.kabengosafaris.Safari.SafariVehicle.Enums.SafariVehicleStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SafariVehicleDTO {

    private String id;
    private String safariId;
    private String safariName;
    private String safariCode;
    private LocalDate safariStartDate;
    private LocalDate safariEndDate;
    private String vehicleId;
    private String vehicleName;
    private String vehicleRegistrationNumber;
    private String vehicleTypeDisplayName;
    private Integer vehicleCapacity;
    private LocalDate startDate;
    private LocalDate endDate;
    private String driverId;
    private String driverFullName;
    private String driverPhone;
    private String assignmentNotes;
    private SafariVehicleStatus status;
    private String statusDisplayName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
