package com.itineraryledger.kabengosafaris.CreditNote.DTOs;

import com.itineraryledger.kabengosafaris.CreditNote.Enums.ConsumptionMethod;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for CreditNote state transition requests
 *
 * Used for workflow transitions: DRAFT -> CONFIRMED -> SENT -> CONSUMED
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreditNoteStateTransitionDTO {

    /**
     * Reason for the state transition
     */
    private String reason;

    /**
     * Additional notes for the transition
     */
    private String notes;

    /**
     * How the credit was consumed (required when transitioning to CONSUMED)
     * Examples: REFUNDED, APPLIED_TO_INVOICE, APPLIED_TO_SAFARI
     */
    private ConsumptionMethod consumptionMethod;

    /**
     * Additional notes about the consumption
     * Example: "Refund sent via bank transfer ref #12345"
     */
    private String consumptionNotes;
}
