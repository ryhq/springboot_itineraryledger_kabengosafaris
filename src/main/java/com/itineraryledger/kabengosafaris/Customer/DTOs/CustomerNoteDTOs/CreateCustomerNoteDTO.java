package com.itineraryledger.kabengosafaris.Customer.DTOs.CustomerNoteDTOs;

import com.itineraryledger.kabengosafaris.Customer.Entity.CustomerNote.NoteType;
import com.itineraryledger.kabengosafaris.Customer.Entity.CustomerNote.NotePriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * CreateCustomerNoteDTO - Request DTO for creating customer notes
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateCustomerNoteDTO {

    @NotBlank(message = "Customer ID is required")
    private String customerId; // Encoded customer ID

    @Builder.Default
    private NoteType noteType = NoteType.GENERAL;

    @Size(max = 200, message = "Subject must not exceed 200 characters")
    private String subject;

    @NotBlank(message = "Note content is required")
    private String content;

    // Follow-up
    private LocalDateTime followUpDate;

    // Visibility & Priority
    @Builder.Default
    private Boolean isPinned = false;

    @Builder.Default
    private Boolean isPrivate = false;

    @Builder.Default
    private NotePriority priority = NotePriority.NORMAL;
}
