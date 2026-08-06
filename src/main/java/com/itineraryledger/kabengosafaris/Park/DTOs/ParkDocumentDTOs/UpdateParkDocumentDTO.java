package com.itineraryledger.kabengosafaris.Park.DTOs.ParkDocumentDTOs;

import java.time.LocalDateTime;

import com.itineraryledger.kabengosafaris.Park.Entities.ParkDocument.DocumentType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for updating ParkDocument metadata.
 * Note: To replace the actual document file, a new upload is required.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateParkDocumentDTO {

    private String title;
    /*
     * Enums arrive as Strings so a blank can CLEAR the field; null still means
     * "leave unchanged". Bound as the enum itself, an empty value makes Jackson
     * reject the whole request body, which is how a set value became impossible
     * to unset (see the charging-basis fix).
     */
    private String documentType;
    private String description;
    private String version;
    private String notes;
    private LocalDateTime validFrom;
    private LocalDateTime validTo;
    private Boolean isActive;
}
