package com.itineraryledger.kabengosafaris.Driver.DTOs;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.itineraryledger.kabengosafaris.Driver.Enums.DriverStatus;
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
public class DriverDTO {

    private String id;
    private String firstName;
    private String lastName;
    private String fullName;
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
    private String statusDisplayName;
    private String notes;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private String nextId;
    private String previousId;
}
