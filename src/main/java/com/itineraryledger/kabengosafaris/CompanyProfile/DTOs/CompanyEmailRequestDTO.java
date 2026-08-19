package com.itineraryledger.kabengosafaris.CompanyProfile.DTOs;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** Create or patch one company email. On create, `email` is required. */
@Data
public class CompanyEmailRequestDTO {

    @Email(message = "Enter a valid email address")
    @Size(max = 255)
    private String email;

    /** GENERAL · RESERVATIONS · BILLING · SUPPORT · MARKETING · MANAGEMENT · OTHER */
    private String emailType;

    @Size(max = 100)
    private String label;

    private Boolean isPrimary;
    private Boolean isActive;
    private Integer displayOrder;
}
