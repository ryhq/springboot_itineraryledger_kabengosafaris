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
    /*
     * Enums arrive as Strings so a blank can CLEAR the field; null still means
     * "leave unchanged". Bound as the enum itself, an empty value makes Jackson
     * reject the whole request body, which is how a set value became impossible
     * to unset (see the charging-basis fix).
     */
    private String type;
    private String make;
    private String model;
    private Integer year;
    private String color;
    private Integer capacity;
    private String fuelType;
    private Long mileage;
    private LocalDate insuranceExpiryDate;
    private LocalDate inspectionExpiryDate;
    private Boolean isActive;
    private String notes;
}
