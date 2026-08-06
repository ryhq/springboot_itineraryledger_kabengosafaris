package com.itineraryledger.kabengosafaris.Expense.DTOs.ExpenseDocumentDTOs;

import com.itineraryledger.kabengosafaris.Expense.Entity.ExpenseDocument;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** Metadata-only update; to replace the file, delete and re-upload. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateExpenseDocumentDTO {
    private String title;
    /*
     * Enums arrive as Strings so a blank can CLEAR the field; null still means
     * "leave unchanged". Bound as the enum itself, an empty value makes Jackson
     * reject the whole request body, which is how a set value became impossible
     * to unset (see the charging-basis fix).
     */
    private String documentType;
    private String description;
    private String documentNumber;
    private String version;
    private String notes;
    private LocalDateTime validFrom;
    private LocalDateTime validTo;
    private Boolean isActive;
    /** Pass "" to detach from a payment, or a new payment id to relink. null = leave unchanged. */
    private String expensePaymentId;
}
