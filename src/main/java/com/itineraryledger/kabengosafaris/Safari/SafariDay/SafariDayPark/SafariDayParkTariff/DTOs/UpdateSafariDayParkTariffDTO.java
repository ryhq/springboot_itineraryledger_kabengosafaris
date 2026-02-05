package com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayPark.SafariDayParkTariff.DTOs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * UpdateSafariDayParkTariffDTO - Data Transfer Object for updating SafariDayParkTariff
 *
 * Note: parkTariff cannot be changed after creation. To change the tariff,
 * delete this record and create a new one.
 *
 * Supports dual update modes:
 * - Planning updates (notes, isIncludedInPrice) require editable safari state
 * - Operational updates (payment details, waiver status) allowed anytime
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateSafariDayParkTariffDTO {

    // Planning fields (require editable safari)
    private String notes;
    private Boolean isIncludedInPrice;

    // Safari-specific operational fields (allowed anytime)
    private Boolean isPaid;
    private String receiptNumber;
    private String paymentNotes;
    private Integer paxCount;
    private Boolean isWaived;
    private String waiverReason;
}
