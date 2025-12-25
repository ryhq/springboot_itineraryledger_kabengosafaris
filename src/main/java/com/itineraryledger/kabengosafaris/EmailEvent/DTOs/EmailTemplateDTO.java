package com.itineraryledger.kabengosafaris.EmailEvent.DTOs;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for EmailTemplate responses
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EmailTemplateDTO {

    /**
     * Obfuscated template ID
     */
    private String id;

    /**
     * Obfuscated email event ID
     */
    private String emailEventId;

    /**
     * Email event name
     */
    private String emailEventName;

    /**
     * Template name
     */
    private String name;

    /**
     * Template description
     */
    private String description;

    /**
     * File name of the HTML template
     */
    private String fileName;

    /**
     * Whether this is the default template for the event
     */
    private Boolean isDefault;

    /**
     * Whether this is a system-generated default template
     */
    private Boolean isSystemDefault;

    /**
     * Whether this template is enabled
     */
    private Boolean enabled;

    /**
     * Size of the template file in bytes
     */
    private Long fileSize;

    /**
     * Human-readable file size (e.g., "5.2 KB")
     */
    private String fileSizeFormatted;

    /**
     * Template content (HTML/text)
     * Only included when explicitly requested
     */
    private String content;

    /**
     * Timestamp when the template was created
     */
    private LocalDateTime createdAt;

    /**
     * Timestamp when the template was last updated
     */
    private LocalDateTime updatedAt;
}
