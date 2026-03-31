package com.itineraryledger.kabengosafaris.VehicleHire.DTOs.VehicleHireDTOs;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.itineraryledger.kabengosafaris.VehicleHire.Enums.HireStatus;
import com.itineraryledger.kabengosafaris.VehicleHire.Enums.PaymentStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VehicleHireDTO {
    private String id;
    private String vehicleId;
    private String vehicleName;
    private String vehicleRegistrationNumber;
    private String vehicleTypeDisplayName;
    private String clientName;
    private String clientPhone;
    private String clientEmail;
    private LocalDate startDate;
    private LocalDate endDate;
    private String pickupLocation;
    private String dropoffLocation;
    private BigDecimal dailyRate;
    private BigDecimal totalAmount;
    private String currency;
    private HireStatus status;
    private String statusDisplayName;
    private PaymentStatus paymentStatus;
    private String paymentStatusDisplayName;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
