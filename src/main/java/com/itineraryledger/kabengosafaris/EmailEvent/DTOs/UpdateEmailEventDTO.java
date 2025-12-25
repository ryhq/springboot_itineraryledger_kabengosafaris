package com.itineraryledger.kabengosafaris.EmailEvent.DTOs;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for updating EmailEvent
 * Only description and enabled status can be updated
 * The name cannot be changed as it's used as a unique identifier
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateEmailEventDTO {

    /**
     * Updated description (optional)
     */
    private String description;

    /**
     * Updated enabled status (optional)
     */
    private Boolean enabled;
}
