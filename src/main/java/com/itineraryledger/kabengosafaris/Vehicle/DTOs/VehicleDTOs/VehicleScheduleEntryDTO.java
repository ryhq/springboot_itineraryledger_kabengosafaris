package com.itineraryledger.kabengosafaris.Vehicle.DTOs.VehicleDTOs;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VehicleScheduleEntryDTO {
    private String id;
    private String type;          // "SAFARI" or "HIRE"
    private LocalDate startDate;
    private LocalDate endDate;
    private String description;
    private String status;
    private String referenceId;
    private String referenceName;

    // Driver info
    private String driverId;
    private String driverName;
    private String driverPhone;

    // Rental client info (hires only)
    private String rentalClientId;
    private String rentalClientName;
    private String rentalClientPhone;
    private String rentalClientEmail;
    private String rentalClientType;

    // Financial info (hires only)
    private BigDecimal dailyRate;
    private BigDecimal totalAmount;
    private String currency;
    private String paymentStatus;

    // Location info (hires only)
    private String pickupLocation;
    private String dropoffLocation;

    // Safari-specific info
    private String safariId;
    private String safariCode;
    private String safariName;
}
