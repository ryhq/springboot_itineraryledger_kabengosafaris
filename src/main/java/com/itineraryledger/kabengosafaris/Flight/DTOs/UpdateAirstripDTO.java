package com.itineraryledger.kabengosafaris.Flight.DTOs;

import java.math.BigDecimal;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Null means "leave alone" on every field here — the house patch semantics. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateAirstripDTO {
    @Size(max = 10) private String code;
    @Size(max = 150) private String name;
    @Size(max = 100) private String region;
    @Size(max = 100) private String district;
    @Size(max = 100) private String country;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String notes;
    private Boolean isActive;
}
