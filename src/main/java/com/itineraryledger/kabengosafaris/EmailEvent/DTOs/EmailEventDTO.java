package com.itineraryledger.kabengosafaris.EmailEvent.DTOs;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for EmailEvent responses
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailEventDTO {

    /**
     * Obfuscated event ID
     */
    private String id;

    /**
     * Unique name of the event
     */
    private String name;

    /**
     * Description of the event
     */
    private String description;

    /**
     * Whether this event is enabled
     */
    private Boolean enabled;

    /**
     * System-defined variables for this event (immutable via API)
     * JSON array defining what variables templates must use
     * Example: [{"name":"username","description":"User's username","isRequired":true}]
     */
    private String variablesJson;

    /**
     * Number of templates associated with this event
     */
    private Long templateCount;

    /**
     * Whether this event has a system default template
     */
    private Boolean hasSystemDefaultTemplate;

    /**
     * Timestamp when the event was created
     */
    private LocalDateTime createdAt;

    /**
     * Timestamp when the event was last updated
     */
    private LocalDateTime updatedAt;
}
