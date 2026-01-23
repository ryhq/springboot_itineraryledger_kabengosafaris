package com.itineraryledger.kabengosafaris.Customer.DTOs.CustomerNoteDTOs;

import com.itineraryledger.kabengosafaris.Customer.Entity.CustomerNote.NoteType;
import com.itineraryledger.kabengosafaris.Customer.Entity.CustomerNote.NotePriority;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * UpdateCustomerNoteDTO - Request DTO for updating customer notes
 * All fields are optional - only provided fields will be updated
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCustomerNoteDTO {

    private NoteType noteType;

    @Size(max = 200, message = "Subject must not exceed 200 characters")
    private String subject;

    private String content;

    // Follow-up
    private LocalDateTime followUpDate;

    private Boolean followUpCompleted;

    // Visibility & Priority
    private Boolean isPinned;

    private Boolean isPrivate;

    private NotePriority priority;

    // Related entities
    @Size(max = 50, message = "Related entity type must not exceed 50 characters")
    private String relatedEntityType;

    private String relatedEntityId; // Obfuscated ID of related entity
}
