package com.itineraryledger.kabengosafaris.Customer.DTOs.CustomerNoteDTOs;

import com.itineraryledger.kabengosafaris.Customer.Entity.CustomerNote.NoteType;
import com.itineraryledger.kabengosafaris.Customer.Entity.CustomerNote.NotePriority;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * UpdateCustomerNoteDTO - Request DTO for updating customer notes
 * All fields are optional - only provided fields will be updated
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCustomerNoteDTO {

    /*
     * Enums arrive as Strings so a blank can CLEAR the field; null still means
     * "leave unchanged". Bound as the enum itself, an empty value makes Jackson
     * reject the whole request body, which is how a set value became impossible
     * to unset (see the charging-basis fix).
     */
    private String noteType;

    @Size(max = 200, message = "Subject must not exceed 200 characters")
    private String subject;

    private String content;

    // Visibility & Priority
    private Boolean isPinned;

    private Boolean isPrivate;

    private String priority;
}
