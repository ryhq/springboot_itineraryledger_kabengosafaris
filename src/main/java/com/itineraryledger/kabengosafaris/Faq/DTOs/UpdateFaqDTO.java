package com.itineraryledger.kabengosafaris.Faq.DTOs;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Patch semantics: null means "leave it alone". */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateFaqDTO {
    private String question;
    private String answer;
    @Size(max = 120, message = "Category must be at most 120 characters")
    private String category;
    private Integer displayOrder;
    private Boolean isActive;
}
