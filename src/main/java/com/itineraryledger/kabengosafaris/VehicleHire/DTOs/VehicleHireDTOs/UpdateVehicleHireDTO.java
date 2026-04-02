package com.itineraryledger.kabengosafaris.VehicleHire.DTOs.VehicleHireDTOs;

import com.itineraryledger.kabengosafaris.VehicleHire.Enums.HireStatus;
import com.itineraryledger.kabengosafaris.VehicleHire.Enums.PaymentStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class UpdateVehicleHireDTO {
    private String vehicleId;
    private String rentalClientId;
    private LocalDate startDate;
    private LocalDate endDate;
    private String pickupLocation;
    private String dropoffLocation;
    private BigDecimal dailyRate;
    private BigDecimal totalAmount;
    private String currency;
    private HireStatus status;
    private PaymentStatus paymentStatus;
    private String notes;
}
