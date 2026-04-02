package com.itineraryledger.kabengosafaris.Driver.DTOs;

import com.itineraryledger.kabengosafaris.Driver.Enums.DriverStatus;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateDriverDTO {

    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    private String phone;
    private String email;
    private String licenseNumber;
    private LocalDate licenseExpiryDate;
    private String licenseClass;
    private String talaLicenseNumber;
    private LocalDate talaExpiryDate;
    private String tourGuideId;
    private LocalDate tourGuideIdExpiryDate;
    private DriverStatus status;
    private String notes;
    private Boolean isActive;
}
