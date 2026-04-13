package com.itineraryledger.kabengosafaris.CreditNote.DTOs;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for updating an existing CreditNote
 *
 * All fields are optional for partial updates.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateCreditNoteDTO {

    private String invoiceId;
    private String title;
    private String description;
    private String reason;

    private BigDecimal taxPercentage;

    private LocalDate issueDate;

    private String internalNotes;
    private String customerNotes;

    private Boolean isActive;
}
