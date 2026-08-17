package com.itineraryledger.kabengosafaris.Faq.DTOs;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A new question.
 *
 * No displayOrder: a new FAQ goes to the end of the list, and where it should actually sit is
 * decided by dragging it there — see UpdateFaqDTO for why one row cannot own a sequence.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateFaqDTO {

    @NotBlank(message = "Question is required")
    private String question;

    @NotBlank(message = "Answer is required")
    private String answer;

    @Size(max = 120, message = "Category must be at most 120 characters")
    private String category;

    private Boolean isActive;
}
