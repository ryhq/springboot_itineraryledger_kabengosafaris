package com.itineraryledger.kabengosafaris.Inclusion.DTOs;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * The new order of the whole catalogue: position IS the new displayOrder (1-indexed).
 *
 * A bare list of ids, because the catalogue is global — there is no parent to scope it to.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReorderInclusionItemsDTO {

    @NotNull(message = "Order list is required")
    @NotEmpty(message = "Order list cannot be empty")
    private List<String> itemOrder;
}
