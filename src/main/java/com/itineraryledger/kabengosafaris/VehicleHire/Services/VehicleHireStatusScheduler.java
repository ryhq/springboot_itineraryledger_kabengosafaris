package com.itineraryledger.kabengosafaris.VehicleHire.Services;

import com.itineraryledger.kabengosafaris.VehicleHire.Entity.VehicleHire;
import com.itineraryledger.kabengosafaris.VehicleHire.Enums.HireStatus;
import com.itineraryledger.kabengosafaris.VehicleHire.Repository.VehicleHireRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Moves a hire through its dates without anyone remembering to.
 *
 * A hire that started yesterday is not "confirmed", it is out; one that ended
 * last week is not "in progress", it is finished. Left to hand, those statuses
 * drift and the fleet timeline shows vehicles as busy that are back on the yard.
 *
 * Only the two automatic transitions are made:
 *   CONFIRMED  → IN_PROGRESS  once the start date has arrived
 *   IN_PROGRESS → COMPLETED   once the end date has passed
 *
 * PENDING is left alone on purpose — an unconfirmed booking starting today is a
 * question for a human, not something to quietly mark as under way. CANCELLED is
 * never touched.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class VehicleHireStatusScheduler {

    private final VehicleHireRepository vehicleHireRepository;

    /** Runs a few minutes after midnight, when "today" has actually changed. */
    @Scheduled(cron = "0 5 0 * * *")
    @Transactional
    public void rollHireStatuses() {
        LocalDate today = LocalDate.now();
        int started = 0;
        int finished = 0;

        List<VehicleHire> hires = vehicleHireRepository.findAll();
        for (VehicleHire hire : hires) {
            HireStatus status = hire.getStatus();
            if (status == null || status == HireStatus.CANCELLED || status == HireStatus.COMPLETED) continue;

            if (status == HireStatus.IN_PROGRESS
                && hire.getEndDate() != null && hire.getEndDate().isBefore(today)) {
                hire.setStatus(HireStatus.COMPLETED);
                vehicleHireRepository.save(hire);
                finished++;
            } else if (status == HireStatus.CONFIRMED
                && hire.getStartDate() != null && !hire.getStartDate().isAfter(today)
                && (hire.getEndDate() == null || !hire.getEndDate().isBefore(today))) {
                hire.setStatus(HireStatus.IN_PROGRESS);
                vehicleHireRepository.save(hire);
                started++;
            }
        }

        if (started > 0 || finished > 0) {
            log.info("Hire statuses rolled: {} started, {} completed", started, finished);
        }
    }
}
