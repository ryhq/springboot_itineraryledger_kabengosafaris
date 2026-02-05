package com.itineraryledger.kabengosafaris.Safari.CostEstimation.Services.Core;

import com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.Services.Core.ParkTariffRateLookupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Park tariff rate lookup service for Safari cost estimation.
 *
 * Delegates to the shared ParkTariffRateLookupService since park tariff
 * rate lookup logic is the same for both Itinerary and Safari.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SafariParkTariffRateLookupService {

    private final ParkTariffRateLookupService parkTariffRateLookupService;

    /**
     * Delegate to shared park tariff rate lookup service.
     */
    public ParkTariffRateLookupService getParkTariffRateLookup() {
        return parkTariffRateLookupService;
    }
}
