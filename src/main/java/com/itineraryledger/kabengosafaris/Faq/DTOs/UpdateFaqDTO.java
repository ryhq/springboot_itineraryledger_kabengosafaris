package com.itineraryledger.kabengosafaris.Faq.DTOs;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Patch semantics: null means "leave it alone".
 *
 * NO displayOrder. The running order is a SEQUENCE, and the only honest way to change a
 * sequence is to state the whole thing — POST /api/faqs/reorder. Editing one row's number
 * left the others holding their old ones, which is how two questions ended up sharing a
 * position and the page order stopped matching what anybody had set.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateFaqDTO {
    private String question;
    private String answer;
    @Size(max = 120, message = "Category must be at most 120 characters")
    private String category;
    private Boolean isActive;
}
