package com.itineraryledger.kabengosafaris.Safari.CostEstimation.Services.Core;

import com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.Services.Core.ActivityRateLookupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Activity rate lookup service for Safari cost estimation.
 *
 * Delegates to the shared ActivityRateLookupService since activity
 * rate lookup logic is the same for both Itinerary and Safari.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SafariActivityRateLookupService {

    private final ActivityRateLookupService activityRateLookupService;

    /**
     * Delegate to shared activity rate lookup service.
     */
    public ActivityRateLookupService getActivityRateLookup() {
        return activityRateLookupService;
    }
}
