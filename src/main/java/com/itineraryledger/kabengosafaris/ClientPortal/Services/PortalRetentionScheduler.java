package com.itineraryledger.kabengosafaris.ClientPortal.Services;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.ClientPortal.Entity.PortalJourney;
import com.itineraryledger.kabengosafaris.ClientPortal.Repository.PortalAccessEventRepository;
import com.itineraryledger.kabengosafaris.ClientPortal.Repository.PortalCodeRepository;
import com.itineraryledger.kabengosafaris.ClientPortal.Repository.PortalDeviceRepository;
import com.itineraryledger.kabengosafaris.ClientPortal.Repository.PortalJourneyRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Behavioural data stops being kept three months after the trip ends.
 *
 * <p>Which sections a named client opened, from which address, on which device, is personal data
 * about an identified individual, and a good share of these people are in the EU. Collecting it
 * is defensible; keeping it forever because nobody wrote the deletion is not, and "we will decide
 * the retention later" has never once been a thing anybody came back to.
 *
 * <p>So the date is decided when the journey closes and stored on it, and this job does what the
 * privacy notice says we do. The anchor is the safari's end, or the day the booking fell through.
 *
 * <p><strong>What is destroyed:</strong> access events, trusted devices and verification codes.
 * <strong>What is not:</strong> the quotation, the invoice, the safari and the journey row
 * itself. Those are the record of a commercial relationship, several of them are required to be
 * kept, and none of them is a record of somebody's browsing.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PortalRetentionScheduler {

    private final PortalJourneyRepository journeys;
    private final PortalAccessEventRepository events;
    private final PortalDeviceRepository devices;
    private final PortalCodeRepository codes;
    private final PortalJourneyService journeyService;

    @Value("${portal.retention.enabled:true}")
    private boolean enabled;

    /** Half past three, after the backup at two and before anybody is at a desk. */
    @Scheduled(cron = "${portal.retention.cron:0 30 3 * * ?}")
    @Transactional
    public void purgeExpiredBehaviouralData() {
        if (!enabled) {
            log.debug("Portal retention is switched off; nothing purged");
            return;
        }
        closeFinishedJourneys(LocalDate.now());
        run(LocalDate.now());
    }

    /**
     * Start the clock on journeys whose trip is over, or whose booking fell through.
     *
     * <p>This is the step the retention promise depends on, and it is easy to leave out: nothing
     * in the ordinary life of a booking has any reason to think about a portal link, so if
     * closing were left to the eight places a quote or a safari changes state, it would be
     * forgotten in at least one of them and those journeys would keep their browsing history for
     * ever. A sweep over the two ends is harder to forget and easier to reason about.
     *
     * @return how many were closed
     */
    @Transactional
    public int closeFinishedJourneys(LocalDate on) {
        int closed = 0;
        for (PortalJourney journey : journeys.dueToClose(on)) {
            try {
                /*
                 * The anchor is the end of the trip where there was one, and today where the
                 * booking simply stopped. Both are "the last day this mattered to the client".
                 */
                LocalDate anchor = journey.getSafari() != null && journey.getSafari().getEndDate() != null
                    ? journey.getSafari().getEndDate()
                    : on;
                journeyService.close(journey, anchor);
                closed++;
            } catch (Exception e) {
                log.error("Could not close portal journey {}: {}", journey.getId(), e.getMessage(), e);
            }
        }
        if (closed > 0) log.info("Closed {} finished portal journey(s)", closed);
        return closed;
    }

    /**
     * The same work, for a given day, so it can be tested and so an operator can run it.
     *
     * @return how many journeys were cleared
     */
    @Transactional
    public int run(LocalDate on) {
        List<PortalJourney> due = journeys.dueForPurge(on);
        if (due.isEmpty()) {
            /* Codes expire in minutes, so there is always a little to sweep even on a quiet day. */
            codes.deleteExpiredBefore(on.atStartOfDay().minusDays(1));
            return 0;
        }

        int cleared = 0;
        for (PortalJourney journey : due) {
            try {
                int removedEvents = events.deleteEveryEventOf(journey.getId());
                codes.deleteEveryCodeOf(journey.getId());
                devices.deleteEveryDeviceOf(journey.getId());

                /*
                 * The date is cleared so the journey is not reconsidered every night for the rest
                 * of its life. The journey itself stays: it is how an invoice is still reachable
                 * by the person who has to pay it.
                 */
                journey.setPurgeAfter(null);
                journeys.save(journey);

                cleared++;
                log.info("Portal journey {}: destroyed {} access event(s) and every device, "
                    + "three months after it closed", journey.getId(), removedEvents);
            } catch (Exception e) {
                /*
                 * One journey failing must not leave the rest unpurged. A retention job that
                 * stops at the first error quietly keeps everything after it.
                 */
                log.error("Could not purge portal journey {}: {}", journey.getId(), e.getMessage(), e);
            }
        }
        return cleared;
    }
}
