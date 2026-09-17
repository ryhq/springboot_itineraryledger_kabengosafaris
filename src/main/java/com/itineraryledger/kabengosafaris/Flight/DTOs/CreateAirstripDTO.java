package com.itineraryledger.kabengosafaris.Flight.DTOs;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateAirstripDTO {

    /** The airline's own three-letter code. The natural key, and how a bundle matches between companies. */
    @NotBlank(message = "Airstrip code is required")
    @Size(max = 10, message = "Airstrip code must be 10 characters or fewer")
    private String code;

    @NotBlank(message = "Airstrip name is required")
    @Size(max = 150)
    private String name;

    @Size(max = 100) private String region;
    @Size(max = 100) private String district;
    @Size(max = 100) private String country;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String notes;
    private Boolean isActive;
}
