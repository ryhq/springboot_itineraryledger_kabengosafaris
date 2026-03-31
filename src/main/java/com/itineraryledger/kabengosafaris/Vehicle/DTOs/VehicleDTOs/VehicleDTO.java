package com.itineraryledger.kabengosafaris.Vehicle.DTOs.VehicleDTOs;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.itineraryledger.kabengosafaris.Vehicle.Enums.FuelType;
import com.itineraryledger.kabengosafaris.Vehicle.Enums.VehicleType;
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
public class VehicleDTO {

    private String id;
    private String name;
    private String registrationNumber;
    private VehicleType type;
    private String typeDisplayName;
    private String typeDescription;
    private String make;
    private String model;
    private Integer year;
    private String color;
    private Integer capacity;
    private FuelType fuelType;
    private String fuelTypeDisplayName;
    private Long mileage;
    private LocalDate insuranceExpiryDate;
    private LocalDate inspectionExpiryDate;
    private Boolean isActive;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
