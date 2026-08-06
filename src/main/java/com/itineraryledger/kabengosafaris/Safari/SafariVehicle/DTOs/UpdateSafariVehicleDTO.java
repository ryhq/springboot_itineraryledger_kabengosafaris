package com.itineraryledger.kabengosafaris.Safari.SafariVehicle.DTOs;

import com.itineraryledger.kabengosafaris.Safari.SafariVehicle.Enums.SafariVehicleStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateSafariVehicleDTO {

    private String vehicleId;
    private LocalDate startDate;
    private LocalDate endDate;
    private String driverId;
    private String assignmentNotes;
    /*
     * Enums arrive as Strings so a blank can CLEAR the field; null still means
     * "leave unchanged". Bound as the enum itself, an empty value makes Jackson
     * reject the whole request body, which is how a set value became impossible
     * to unset (see the charging-basis fix).
     */
    private String status;
}
