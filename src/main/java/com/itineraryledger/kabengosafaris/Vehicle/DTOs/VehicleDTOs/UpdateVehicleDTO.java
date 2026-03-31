package com.itineraryledger.kabengosafaris.Vehicle.DTOs.VehicleDTOs;

import com.itineraryledger.kabengosafaris.Vehicle.Enums.FuelType;
import com.itineraryledger.kabengosafaris.Vehicle.Enums.VehicleType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateVehicleDTO {

    private String name;
    private String registrationNumber;
    private VehicleType type;
    private String make;
    private String model;
    private Integer year;
    private String color;
    private Integer capacity;
    private FuelType fuelType;
    private Long mileage;
    private LocalDate insuranceExpiryDate;
    private LocalDate inspectionExpiryDate;
    private Boolean isActive;
    private String notes;
}
