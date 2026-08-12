package com.itineraryledger.kabengosafaris.Park.DTOs;

import com.itineraryledger.kabengosafaris.Park.ParkType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * CreateParkDTO - Data Transfer Object for creating a new Park
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateParkDTO {

    @NotBlank(message = "Park name is required")
    private String name;
    private String slug;
    @NotNull(message = "Park type is required")
    private ParkType parkType;
    private String region;
    private String district;
    private String location;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String elevation;
    private String size;
    private String shortDescription;
    private String fullDescription;
    private String history;
    private String ecosystem;
    private String wildlife;
    private String vegetation;
    private String primaryImage;
    private String bestTimeToVisit;
    private String openingHours;
    private String accessInformation;
    private String tags;
    private Boolean isActive;
    private Boolean isWebActive;

    /** obfuscated vendor id — who charges to enter */
    private String vendorId;
}
