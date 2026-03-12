package com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.Services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ItineraryCostScheduler {

    private final ItineraryCostPersistenceService itineraryCostPersistenceService;

    @Scheduled(cron = "${itinerary.cost.schedule.cron:0 0 3 * * ?}")
    public void recalculatePublishedCosts() {
        log.info("Starting scheduled itinerary cost recalculation");
        itineraryCostPersistenceService.persistAllPublished();
        log.info("Scheduled itinerary cost recalculation finished");
    }
}
