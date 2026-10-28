package com.itineraryledger.kabengosafaris.Quote;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.itineraryledger.kabengosafaris.Accommodation.Entities.Accommodation;
import com.itineraryledger.kabengosafaris.Park.Park;
import com.itineraryledger.kabengosafaris.Quote.Entity.Quote;
import com.itineraryledger.kabengosafaris.Quote.QuoteDay.Entity.QuoteDay;
import com.itineraryledger.kabengosafaris.Quote.QuoteDay.QuoteDayAccommodation.Entity.QuoteDayAccommodation;
import com.itineraryledger.kabengosafaris.Quote.QuoteDay.QuoteDayPark.Entity.QuoteDayPark;
import com.itineraryledger.kabengosafaris.Quote.QuoteInclusion.Entity.QuoteInclusion;
import com.itineraryledger.kabengosafaris.Quote.QuotePax.Entity.QuotePax;
import com.itineraryledger.kabengosafaris.Quote.Services.QuoteServices.QuoteTreeCopyService;

/**
 * A new version is the same sale repriced, so it has to carry the whole trip.
 *
 * <p>It did not. {@code QuoteVersionService} copied the line items and stopped: no days, no
 * travelling party, no inclusions. The document therefore read correctly and was hollow
 * underneath, because the day tree is what the price is computed FROM. {@code recalculate}
 * skips a quotation with no days, so a v2 could never be repriced — every edit to it changed
 * the trip and left the total exactly where it was, with no error anywhere.
 *
 * <p>That was survivable while only staff touched a quotation. It stopped being survivable the
 * moment clients could: the portal builds its change history from the day tree, so a v2's
 * history was blank; it builds the editing controls from the day tree, so there was nothing to
 * edit; and a client who did change something would have been shown an unmoved price.
 *
 * <p>The other half of the rule is the direction of the copy, and it is the half that would be
 * easy to get wrong while fixing the first. A new version is copied from the previous QUOTE and
 * never re-snapshotted from the itinerary.
 */
class ANewVersionCarriesTheWholeTripTest {

    private QuoteTreeCopyService service;
    private Quote original;

    private Accommodation kudu;
    private Accommodation farmOfDreams;
    private Park serengeti;

    @BeforeEach
    void setUp() {
        service = new QuoteTreeCopyService();

        kudu = new Accommodation();
        kudu.setName("Kudu Lodge");
        farmOfDreams = new Accommodation();
        farmOfDreams.setName("Farm of Dreams Lodge");
        serengeti = new Park();
        serengeti.setName("Serengeti");

        original = new Quote();
        original.setId(1L);
        original.setQuoteCode("QT-1046-0926-1");
        original.setDays(new ArrayList<>());
        original.setPaxList(new ArrayList<>());
        original.setInclusionList(new ArrayList<>());

        QuoteDay day = QuoteDay.builder()
            .dayNumber(3).title("Serengeti").endLocation("Seronera").build();
        day.setId(100L);

        /* what they booked, and the alternative we offered beside it */
        day.addAccommodation(QuoteDayAccommodation.builder()
            .accommodation(kudu).roomCount(2).isAlternative(false).build());
        day.addAccommodation(QuoteDayAccommodation.builder()
            .accommodation(farmOfDreams).roomCount(2).isAlternative(true).build());

        QuoteDayPark park = QuoteDayPark.builder().park(serengeti).sortOrder(1).build();
        day.addPark(park);

        day.setQuote(original);
        original.getDays().add(day);

        original.getPaxList().add(QuotePax.builder()
            .quote(original).count(2).notes("honeymoon").build());

        original.addInclusion(QuoteInclusion.builder()
            .label("Park fees").isIncluded(true).sortOrder(1).build());
    }

    private Quote emptyVersion() {
        Quote next = new Quote();
        next.setId(2L);
        next.setQuoteCode("QT-1046-0926-2");
        next.setDays(new ArrayList<>());
        next.setPaxList(new ArrayList<>());
        next.setInclusionList(new ArrayList<>());
        return next;
    }

    // ------------------------------------------------------------------ what travels

    @Test
    @DisplayName("the day tree travels, which is the thing the price is computed from")
    void theDayTreeTravels() {
        Quote next = emptyVersion();
        assertEquals(1, service.copyTree(original, next));

        assertEquals(1, next.getDays().size(), "a version with no days can never be repriced");
        assertEquals(3, next.getDays().get(0).getDayNumber());
        assertEquals("Seronera", next.getDays().get(0).getEndLocation());
        assertSame(next, next.getDays().get(0).getQuote(), "the copy belongs to the new version");
    }

    @Test
    @DisplayName("the alternatives travel, or the client is offered a choice with nothing in it")
    void theAlternativesTravel() {
        Quote next = emptyVersion();
        service.copyTree(original, next);

        List<QuoteDayAccommodation> stays = next.getDays().get(0).getAccommodations();
        assertEquals(2, stays.size());
        assertEquals(1, stays.stream().filter(s -> Boolean.TRUE.equals(s.getIsAlternative())).count(),
            "the alternatives are the menu at editing level CHOOSE_OFFERED");
        assertEquals(2, stays.get(0).getRoomCount(), "and what they chose about it");
    }

    @Test
    @DisplayName("the parks travel, because forgetting a park fee does not look like a bug")
    void theParksTravel() {
        Quote next = emptyVersion();
        service.copyTree(original, next);

        List<QuoteDayPark> parks = next.getDays().get(0).getParks();
        assertEquals(1, parks.size());
        assertSame(serengeti, parks.get(0).getPark());
    }

    @Test
    @DisplayName("the travelling party travels, which is the biggest lever on the price")
    void thePartyTravels() {
        Quote next = emptyVersion();
        service.copyTree(original, next);

        assertEquals(1, next.getPaxList().size());
        assertEquals(2, next.getPaxList().get(0).getCount());
        assertSame(next, next.getPaxList().get(0).getQuote());
    }

    @Test
    @DisplayName("the inclusions travel, so a concession promised to this customer survives")
    void theInclusionsTravel() {
        Quote next = emptyVersion();
        service.copyTree(original, next);

        assertEquals(1, next.getInclusionList().size());
        assertEquals("Park fees", next.getInclusionList().get(0).getLabel());
        assertSame(next, next.getInclusionList().get(0).getQuote());
    }

    // ------------------------------------------------------------------ how it travels

    @Test
    @DisplayName("the copy is a copy: editing the new version does not touch the one it replaced")
    void theCopyIsDeep() {
        Quote next = emptyVersion();
        service.copyTree(original, next);

        assertNotSame(original.getDays().get(0), next.getDays().get(0));
        next.getDays().get(0).getAccommodations().get(0).setRoomCount(9);

        assertEquals(2, original.getDays().get(0).getAccommodations().get(0).getRoomCount(),
            "v1 is a record of what was sent; a later version must not rewrite it");
    }

    @Test
    @DisplayName("catalogue rows travel as references, never as copies of the catalogue")
    void theCatalogueIsNotDuplicated() {
        Quote next = emptyVersion();
        service.copyTree(original, next);

        assertSame(kudu, next.getDays().get(0).getAccommodations().get(0).getAccommodation(),
            "the lodge is a catalogue row shared by every quotation that names it; what is "
                + "copied is the decision to use it");
    }

    @Test
    @DisplayName("an empty previous version copies nothing rather than failing")
    void nothingToCopyIsNotAnError() {
        Quote legacy = new Quote();
        legacy.setId(9L);
        legacy.setQuoteCode("QT-OLD");
        legacy.setDays(new ArrayList<>());
        legacy.setPaxList(new ArrayList<>());
        legacy.setInclusionList(new ArrayList<>());

        assertEquals(0, service.copyTree(legacy, emptyVersion()),
            "quotes written before the day tree existed still have to be versionable");
    }

    // ------------------------------------------------------------------ the direction

    /**
     * The copy reads the previous quotation and nothing else.
     *
     * <p>Re-snapshotting from the itinerary is the obvious shortcut — the code to do it already
     * exists in {@code QuoteFromItineraryGenerationService} — and it is wrong in a way that
     * leaves no trace. After the first edit the two are different documents on purpose: the
     * quotation's day tree is the customer's own copy, which is exactly why the portal is
     * allowed to let them change it. Reading the itinerary would silently discard every choice
     * they made and hand them back the product as it is sold to strangers.
     */
    @Test
    @DisplayName("a new version is copied from the previous quotation, never from the itinerary")
    void theCopyComesFromTheQuoteNotTheProduct() throws IOException {
        String source = Files.readString(Path.of("src/main/java/com/itineraryledger/"
            + "kabengosafaris/Quote/Services/QuoteServices/QuoteTreeCopyService.java"));
        int body = source.indexOf("public class QuoteTreeCopyService");

        assertFalse(source.substring(body).contains("Itinerary"),
            "reading the itinerary here would throw away everything the customer chose");
    }

    @Test
    @DisplayName("creating a version copies the trip, and cannot quietly stop doing so again")
    void versionCreationStillCopiesTheTrip() throws IOException {
        String source = Files.readString(Path.of("src/main/java/com/itineraryledger/"
            + "kabengosafaris/Quote/Services/QuoteServices/QuoteVersionService.java"));

        assertTrue(source.contains("treeCopy.copyTree(original, newQuote)"),
            "a version without the day tree reads correctly and cannot be repriced, which is "
                + "how this went unnoticed for so long");
    }
}
