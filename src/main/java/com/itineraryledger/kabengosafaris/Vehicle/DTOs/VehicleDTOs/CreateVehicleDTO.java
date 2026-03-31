package com.itineraryledger.kabengosafaris.Vehicle.DTOs.VehicleDTOs;

import com.itineraryledger.kabengosafaris.Vehicle.Enums.FuelType;
import com.itineraryledger.kabengosafaris.Vehicle.Enums.VehicleType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateVehicleDTO {

    @NotBlank(message = "Vehicle name is required")
    private String name;

    @NotBlank(message = "Registration number is required")
    private String registrationNumber;

    @NotNull(message = "Vehicle type is required")
    private VehicleType type;

    private String make;
    private String model;
    private Integer year;
    private String color;

    @NotNull(message = "Capacity is required")
    @Min(value = 1, message = "Capacity must be at least 1")
    private Integer capacity;

    private FuelType fuelType;
    private Long mileage;
    private LocalDate insuranceExpiryDate;
    private LocalDate inspectionExpiryDate;
    private Boolean isActive;
    private String notes;
}
