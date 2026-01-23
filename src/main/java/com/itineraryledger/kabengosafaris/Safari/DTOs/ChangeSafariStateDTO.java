package com.itineraryledger.kabengosafaris.Safari.DTOs;

import com.itineraryledger.kabengosafaris.Safari.Enums.SafariState;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ChangeSafariStateDTO - DTO for manually changing Safari state
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChangeSafariStateDTO {

    @NotNull(message = "New state is required")
    private SafariState newState;

    /**
     * Reason for the state change (required for CANCELLED, ON_HOLD, POSTPONED)
     */
    private String reason;
}
