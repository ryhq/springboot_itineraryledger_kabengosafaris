package com.itineraryledger.kabengosafaris.Faq.DTOs;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * The new order of the whole list: position IS the new displayOrder (1-indexed).
 *
 * A bare list of ids, because FAQs are global — there is no parent to scope them to.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReorderFaqsDTO {

    @NotNull(message = "Order list is required")
    @NotEmpty(message = "Order list cannot be empty")
    private List<String> faqOrder;
}
