package com.itineraryledger.kabengosafaris.VehicleHire.DTOs.VehicleHireDTOs;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class CreateVehicleHireDTO {
    @NotBlank(message = "Vehicle ID is required")
    private String vehicleId;
    @NotBlank(message = "Client name is required")
    private String clientName;
    private String clientPhone;
    private String clientEmail;
    @NotNull(message = "Start date is required")
    private LocalDate startDate;
    @NotNull(message = "End date is required")
    private LocalDate endDate;
    private String pickupLocation;
    private String dropoffLocation;
    private BigDecimal dailyRate;
    private BigDecimal totalAmount;
    private String currency;
    private String notes;
}
