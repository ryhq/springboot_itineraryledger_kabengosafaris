package com.itineraryledger.kabengosafaris.ClientPortal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.itineraryledger.kabengosafaris.ClientPortal.Entity.PortalJourney;
import com.itineraryledger.kabengosafaris.ClientPortal.Enums.JourneyStatus;
import com.itineraryledger.kabengosafaris.ClientPortal.Repository.PortalAccessEventRepository;
import com.itineraryledger.kabengosafaris.ClientPortal.Repository.PortalCodeRepository;
import com.itineraryledger.kabengosafaris.ClientPortal.Repository.PortalDeviceRepository;
import com.itineraryledger.kabengosafaris.ClientPortal.Repository.PortalJourneyRepository;
import com.itineraryledger.kabengosafaris.ClientPortal.Services.PortalJourneyService;
import com.itineraryledger.kabengosafaris.ClientPortal.Services.PortalRetentionScheduler;

/**
 * What a client looked at stops being kept three months after their trip.
 *
 * <p>Which sections a named person opened, from which address, on which browser, is personal
 * data about an identified individual, and a good share of these people are in the EU.
 * Collecting it to answer "she says the invoice is not there" is defensible. Keeping it for ever
 * because nobody wrote the deletion is not, and "we will decide retention later" has never once
 * been a thing anybody came back to.
 *
 * <p>Two properties are worth a test rather than a comment. The clock is <strong>stored</strong>
 * when the journey closes, so changing a safari's dates afterwards cannot quietly extend how
 * long somebody's browsing history is held. And the purge takes the behaviour and leaves the
 * paperwork: a quotation and an invoice are the record of a transaction, several of them have to
 * be kept, and none of them is a record of somebody's browsing.
 */
class PortalDataDiesWithTheTripTest {

    private static final Path SCHEDULER = Path.of(
        "src/main/java/com/itineraryledger/kabengosafaris/ClientPortal/Services/PortalRetentionScheduler.java");
    private static final Path JOURNEY_SERVICE = Path.of(
        "src/main/java/com/itineraryledger/kabengosafaris/ClientPortal/Services/PortalJourneyService.java");

    @Test
    @DisplayName("the date is three months after the trip, and it is written down")
    void theClockIsStoredNotDerived() throws Exception {
        String source = Files.readString(JOURNEY_SERVICE);

        assertTrue(source.contains("BEHAVIOURAL_DATA_MONTHS = 3"),
            "three months after the safari ends, or after the booking falls through");
        assertTrue(source.contains("anchor.plusMonths(BEHAVIOURAL_DATA_MONTHS)"),
            "and the date is computed once, from the end of the trip");
        assertTrue(source.contains("journey.setPurgeAfter("),
            "then stored. Re-deriving it from the safari every night would mean a trip whose "
            + "dates move silently extends how long we keep somebody's browsing history, which "
            + "is a promise quietly broken rather than a decision taken");
    }

    @Test
    @DisplayName("the purge destroys the behaviour and keeps the paperwork")
    void itTakesTheRightThings() {
        PortalJourneyRepository journeys = mock(PortalJourneyRepository.class);
        PortalAccessEventRepository events = mock(PortalAccessEventRepository.class);
        PortalDeviceRepository devices = mock(PortalDeviceRepository.class);
        PortalCodeRepository codes = mock(PortalCodeRepository.class);

        PortalJourney due = PortalJourney.builder()
            .id(7L)
            .token("t")
            .status(JourneyStatus.CLOSED)
            .purgeAfter(LocalDate.now().minusDays(1))
            .build();
        when(journeys.dueForPurge(any())).thenReturn(List.of(due));

        PortalRetentionScheduler scheduler =
            new PortalRetentionScheduler(journeys, events, devices, codes,
                mock(PortalJourneyService.class));
        ReflectionTestUtils.setField(scheduler, "enabled", true);

        int cleared = scheduler.run(LocalDate.now());

        assertEquals(1, cleared);
        verify(events).deleteEveryEventOf(7L);
        verify(devices).deleteEveryDeviceOf(7L);
        verify(codes).deleteEveryCodeOf(7L);

        /*
         * The journey row itself survives, and so does everything it points at. A client looks up
         * their own invoice months after the trip, and a deleted journey is a client who cannot.
         */
        verify(journeys, never()).delete(any());
        verify(journeys, never()).deleteById(anyLong());
        assertNull(due.getPurgeAfter(),
            "the date is cleared so the journey is not reconsidered every night for ever");
    }

    @Test
    @DisplayName("one journey failing does not leave the rest unpurged")
    void oneFailureDoesNotStopTheJob() {
        PortalJourneyRepository journeys = mock(PortalJourneyRepository.class);
        PortalAccessEventRepository events = mock(PortalAccessEventRepository.class);
        PortalDeviceRepository devices = mock(PortalDeviceRepository.class);
        PortalCodeRepository codes = mock(PortalCodeRepository.class);

        PortalJourney bad = PortalJourney.builder().id(1L).token("a")
            .purgeAfter(LocalDate.now().minusDays(1)).build();
        PortalJourney good = PortalJourney.builder().id(2L).token("b")
            .purgeAfter(LocalDate.now().minusDays(1)).build();
        when(journeys.dueForPurge(any())).thenReturn(List.of(bad, good));
        when(events.deleteEveryEventOf(1L)).thenThrow(new RuntimeException("row is locked"));

        PortalRetentionScheduler scheduler =
            new PortalRetentionScheduler(journeys, events, devices, codes,
                mock(PortalJourneyService.class));
        ReflectionTestUtils.setField(scheduler, "enabled", true);

        int cleared = scheduler.run(LocalDate.now());

        assertEquals(1, cleared, "the second journey is still purged");
        verify(events).deleteEveryEventOf(2L);
        /*
         * A retention job that stops at the first error quietly keeps everything after it, and
         * the only symptom is data that should have gone months ago.
         */
    }

    @Test
    @DisplayName("a finished trip is closed, which is what starts the clock at all")
    void somethingActuallyClosesAJourney() throws Exception {
        /*
         * The step that is easy to leave out, and whose absence is invisible: without it every
         * journey stays OPEN for ever, purgeAfter is never written, the purge finds nothing, and
         * the privacy notice quietly describes a deletion that does not happen.
         */
        String scheduler = Files.readString(SCHEDULER);
        assertTrue(scheduler.contains("closeFinishedJourneys"),
            "something has to close a journey when the trip ends, or nothing is ever purged");
        assertTrue(scheduler.contains("journeyService.close(journey, anchor)"),
            "and closing is what writes the deletion date");

        String repository = Files.readString(Path.of(
            "src/main/java/com/itineraryledger/kabengosafaris/ClientPortal/Repository/PortalJourneyRepository.java"));
        assertTrue(repository.contains("dueToClose"),
            "the sweep finds the two ends: a safari that has finished, or a quotation that was "
            + "rejected, cancelled or expired with no safari behind it");
        assertTrue(repository.contains("j.safari.endDate < :on"),
            "a trip is over when its last day has passed");
    }

    @Test
    @DisplayName("a journey with no end date is never purged by accident")
    void anOpenJourneyIsLeftAlone() throws Exception {
        String source = Files.readString(SCHEDULER);
        assertTrue(source.contains("dueForPurge"),
            "the job selects by the stored date and nothing else");

        String repository = Files.readString(Path.of(
            "src/main/java/com/itineraryledger/kabengosafaris/ClientPortal/Repository/PortalJourneyRepository.java"));
        assertTrue(repository.contains("j.purgeAfter is not null"),
            "a journey that has not closed has no purge date, and a null must never be read as "
            + "'overdue'. That mistake deletes the access history of every live booking");
        assertFalse(repository.contains("j.purgeAfter <= :on or"),
            "no second condition can widen what the job collects");
    }
}
