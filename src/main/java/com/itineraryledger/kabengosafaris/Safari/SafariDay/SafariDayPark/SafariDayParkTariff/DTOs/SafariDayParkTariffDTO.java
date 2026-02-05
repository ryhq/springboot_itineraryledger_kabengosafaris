package com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayPark.SafariDayParkTariff.DTOs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * SafariDayParkTariffDTO - Data Transfer Object for SafariDayParkTariff entity
 *
 * Includes Safari-specific fields for payment tracking and waiver management.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SafariDayParkTariffDTO {
    private String id;
    private String safariDayParkId;
    private String parkId;
    private String parkName;
    private String tariffId;
    private String tariffName;
    private String notes;
    private Boolean isIncludedInPrice;

    // Safari-specific fields
    private Boolean isPaid;
    private LocalDateTime paidAt;
    private String receiptNumber;
    private String paymentNotes;
    private Integer paxCount;
    private Boolean isWaived;
    private String waiverReason;

    private LocalDateTime createdAt;
}
