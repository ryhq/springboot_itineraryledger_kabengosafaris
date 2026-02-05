package com.itineraryledger.kabengosafaris.Safari.CostEstimation.Services.Core;

import com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.Services.Core.SeasonResolverService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Season resolver service for Safari cost estimation.
 *
 * Delegates to the shared SeasonResolverService since season resolution
 * logic is the same for both Itinerary and Safari.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SafariSeasonResolverService {

    private final SeasonResolverService seasonResolverService;

    /**
     * Delegate to shared season resolver service.
     */
    public SeasonResolverService getSeasonResolver() {
        return seasonResolverService;
    }
}
