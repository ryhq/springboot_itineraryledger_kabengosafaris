package com.itineraryledger.kabengosafaris.EmailEvent.DTOs;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for updating EmailTemplate
 * All fields are optional - only provided fields will be updated
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateEmailTemplateDTO {

    /**
     * Updated template name (optional)
     * If provided, must be unique within the email event
     */
    private String name;

    /**
     * Updated description (optional)
     */
    private String description;

    /**
     * Updated HTML content (optional)
     */
    private String content;

    /**
     * Updated default status (optional)
     */
    private Boolean isDefault;

    /**
     * Updated enabled status (optional)
     */
    private Boolean enabled;
}
