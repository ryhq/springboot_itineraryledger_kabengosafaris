package com.itineraryledger.kabengosafaris.EmailEvent.DTOs;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for creating new EmailTemplate
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateEmailTemplateDTO {

    /**
     * Template name (required)
     * Must be unique within the email event
     */
    @NotBlank(message = "Template name is required")
    private String name;

    /**
     * Template description (optional)
     */
    private String description;

    /**
     * HTML content of the template (required)
     */
    @NotBlank(message = "Template content is required")
    private String content;

    /**
     * Whether this should be the default template (optional, defaults to false)
     */
    private Boolean isDefault;

    /**
     * Whether this template is enabled (optional, defaults to true)
     */
    private Boolean enabled;
}
