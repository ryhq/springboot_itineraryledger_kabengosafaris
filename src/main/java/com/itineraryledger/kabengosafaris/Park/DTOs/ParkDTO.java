package com.itineraryledger.kabengosafaris.Park.DTOs;

import com.itineraryledger.kabengosafaris.Park.ParkType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * ParkDTO - Data Transfer Object for Park entity
 * Contains obfuscated ID for secure data transfer
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ParkDTO {
    private String idObfuscated;
    private String name;
    private String slug;
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
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
