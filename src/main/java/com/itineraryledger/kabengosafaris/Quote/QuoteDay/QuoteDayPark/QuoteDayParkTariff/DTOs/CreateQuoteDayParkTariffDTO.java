package com.itineraryledger.kabengosafaris.Quote.QuoteDay.QuoteDayPark.QuoteDayParkTariff.DTOs;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateQuoteDayParkTariffDTO {

    @NotBlank(message = "Tariff ID is required")
    private String tariffId;

    private String notes;
    private Boolean isIncludedInPrice;
}
