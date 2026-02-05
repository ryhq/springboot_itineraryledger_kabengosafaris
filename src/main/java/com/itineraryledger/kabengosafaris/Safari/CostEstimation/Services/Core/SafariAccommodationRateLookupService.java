package com.itineraryledger.kabengosafaris.Safari.CostEstimation.Services.Core;

import com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.Services.Core.AccommodationRateLookupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Accommodation rate lookup service for Safari cost estimation.
 *
 * Delegates to the shared AccommodationRateLookupService since accommodation
 * rate lookup logic is the same for both Itinerary and Safari.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SafariAccommodationRateLookupService {

    private final AccommodationRateLookupService accommodationRateLookupService;

    /**
     * Delegate to shared accommodation rate lookup service.
     */
    public AccommodationRateLookupService getAccommodationRateLookup() {
        return accommodationRateLookupService;
    }
}
