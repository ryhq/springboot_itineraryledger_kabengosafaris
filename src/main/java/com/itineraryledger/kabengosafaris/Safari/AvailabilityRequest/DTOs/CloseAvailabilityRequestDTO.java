package com.itineraryledger.kabengosafaris.Safari.AvailabilityRequest.DTOs;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CloseAvailabilityRequestDTO {

    /** CONFIRMED | DECLINED | SUPERSEDED | CANCELLED */
    @NotBlank
    private String reason;

    private String notes;
}
