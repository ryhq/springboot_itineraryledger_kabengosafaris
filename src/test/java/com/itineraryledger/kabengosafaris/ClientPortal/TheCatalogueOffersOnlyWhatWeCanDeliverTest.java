package com.itineraryledger.kabengosafaris.ClientPortal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.lang.reflect.RecordComponent;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.itineraryledger.kabengosafaris.Accommodation.Entities.Accommodation;
import com.itineraryledger.kabengosafaris.Accommodation.Entities.AccommodationBoardType;
import com.itineraryledger.kabengosafaris.Accommodation.Entities.AccommodationRate;
import com.itineraryledger.kabengosafaris.Accommodation.Entities.AccommodationRoomStandard;
import com.itineraryledger.kabengosafaris.Accommodation.Entities.AccommodationRoomType;
import com.itineraryledger.kabengosafaris.Accommodation.Repositories.AccommodationRateRepository;
import com.itineraryledger.kabengosafaris.Accommodation.Repositories.AccommodationImageRepository;
import com.itineraryledger.kabengosafaris.Accommodation.Repositories.AccommodationRepository;
import com.itineraryledger.kabengosafaris.ClientPortal.Services.PortalCatalogueService;
import com.itineraryledger.kabengosafaris.ClientPortal.Services.PortalMediaUrls;
import com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.Services.Core.SeasonResolverService;
import com.itineraryledger.kabengosafaris.Quote.Entity.Quote;
import com.itineraryledger.kabengosafaris.Quote.QuoteDay.Entity.QuoteDay;
import com.itineraryledger.kabengosafaris.Quote.QuoteDay.QuoteDayAccommodation.Entity.QuoteDayAccommodation;
import com.itineraryledger.kabengosafaris.Season.Season;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

/**
 * The catalogue offers a client only what we could actually deliver, and tells them no prices.
 *
 * <p>Editing level CHOOSE_CATALOGUE lets a client reach past the alternatives we put on their
 * quotation into everything we hold a rate for. Two bounds make that safe and both are
 * load-bearing.
 *
 * <p><b>Region.</b> The night's route is already fixed: the vehicle arrives from somewhere and
 * leaves for somewhere. A lodge four hours away is not an alternative, it is a different trip,
 * and a client who picks one has been invited to choose something we then have to take back.
 *
 * <p><b>Date.</b> An option with no rate resolving for that night is a price we cannot stand
 * behind. It is not offered at all, rather than offered and refused afterwards.
 *
 * <p>And no figures. Not because the rack rate is secret — it is published — but because it is
 * not what this client would pay: commission and uplift are applied per quotation by the
 * pricing engine. A number that is wrong in a way the reader cannot detect is worse than no
 * number. They choose, the quotation reprices, and the total they are then shown is real.
 */
class TheCatalogueOffersOnlyWhatWeCanDeliverTest {

    private AccommodationRepository accommodations;
    private AccommodationRateRepository rates;
    private SeasonResolverService seasons;
    private PortalCatalogueService service;

    private Quote quote;
    private QuoteDayAccommodation booked;

    @BeforeEach
    void setUp() {
        accommodations = mock(AccommodationRepository.class);
        rates = mock(AccommodationRateRepository.class);
        seasons = mock(SeasonResolverService.class);

        IdObfuscator obfuscator = mock(IdObfuscator.class);
        when(obfuscator.encodeId(anyLong())).thenReturn("opt1");

        /* no photographs in these tests: an empty image list is the ordinary case for a lodge
           nobody has uploaded one for, and it must not change which options are offered */
        AccommodationImageRepository images = mock(AccommodationImageRepository.class);
        when(images.findByAccommodationIdOrderByDisplayOrderAsc(anyLong())).thenReturn(List.of());

        service = new PortalCatalogueService(accommodations, images, new PortalMediaUrls(),
            rates, seasons, obfuscator);

        quote = new Quote();
        quote.setId(1L);
        quote.setQuoteCode("QT-1046-0926-1");
        quote.setSafariStartDate(LocalDate.of(2027, 10, 1));

        QuoteDay day = QuoteDay.builder().dayNumber(3).title("Serengeti").build();
        day.setId(100L);
        day.setQuote(quote);
        quote.setDays(new ArrayList<>(List.of(day)));

        booked = new QuoteDayAccommodation();
        booked.setId(50L);
        booked.setQuoteDay(day);
        booked.setAccommodation(lodge(10L, "Kudu Lodge", "Serengeti"));
        booked.setRoomCount(2);
    }

    private Accommodation lodge(Long id, String name, String region) {
        Accommodation a = new Accommodation();
        a.setId(id);
        a.setName(name);
        a.setRegion(region);
        a.setIsActive(true);
        return a;
    }

    private AccommodationRate rate(Long id, Accommodation on) {
        AccommodationRoomType roomType = new AccommodationRoomType();
        roomType.setName("Double");
        AccommodationRoomStandard standard = new AccommodationRoomStandard();
        standard.setName("Standard");
        AccommodationBoardType board = new AccommodationBoardType();
        board.setName("Full Board");

        AccommodationRate r = new AccommodationRate();
        r.setId(id);
        r.setAccommodation(on);
        r.setRoomType(roomType);
        r.setRoomStandard(standard);
        r.setBoardType(board);
        r.setRackRate(new BigDecimal("420.00"));
        r.setStoRate(new BigDecimal("305.00"));
        r.setIsActive(true);
        return r;
    }

    private Season season(Long id) {
        Season s = new Season();
        s.setId(id);
        s.setName("High Season");
        return s;
    }

    // ------------------------------------------------------------------ the bounds

    @Test
    @DisplayName("only the region the night is already in")
    void boundedByRegion() {
        Accommodation nearby = lodge(11L, "Farm of Dreams Lodge", "Serengeti");
        when(accommodations.findLiveInRegion("Serengeti")).thenReturn(List.of(nearby));
        when(seasons.resolveAccommodationSeason(eq(11L), any())).thenReturn(season(7L));
        when(rates.findByAccommodationIdAndSeasonIdAndIsActiveTrue(11L, 7L))
            .thenReturn(List.of(rate(900L, nearby)));

        List<PortalCatalogueService.Option> options = service.optionsFor(quote, booked);

        assertEquals(1, options.size());
        assertEquals("Farm of Dreams Lodge", options.get(0).accommodation());
        assertEquals("Full Board", options.get(0).board());
    }

    @Test
    @DisplayName("a lodge with nothing priced for that night is not offered at all")
    void anUnpricedNightIsNotAnOption() {
        Accommodation nearby = lodge(11L, "Farm of Dreams Lodge", "Serengeti");
        when(accommodations.findLiveInRegion("Serengeti")).thenReturn(List.of(nearby));
        when(seasons.resolveAccommodationSeason(eq(11L), any())).thenReturn(null);

        assertTrue(service.optionsFor(quote, booked).isEmpty(),
            "offering it and refusing it afterwards is worse than not offering it");
    }

    @Test
    @DisplayName("a quotation with no start date has no catalogue, because it has no season")
    void noDateMeansNoCatalogue() {
        quote.setSafariStartDate(null);

        assertTrue(service.optionsFor(quote, booked).isEmpty(),
            "a price for an unknown date is a guess");
    }

    @Test
    @DisplayName("a lodge with no region of its own gives the search nothing to bound it")
    void noRegionMeansNoCatalogue() {
        booked.getAccommodation().setRegion(null);

        assertTrue(service.optionsFor(quote, booked).isEmpty(),
            "the whole country is not a shortlist");
    }

    @Test
    @DisplayName("the night is the start date plus the day number, as every aggregator computes it")
    void theNightIsTheDaysOwnDate() {
        assertEquals(LocalDate.of(2027, 10, 3),
            service.dateOf(quote, quote.getDays().get(0)),
            "day 3 of a trip starting on the 1st");
    }

    @Test
    @DisplayName("the one they already have is marked rather than offered as a change")
    void theCurrentChoiceIsMarked() {
        Accommodation same = booked.getAccommodation();
        when(accommodations.findLiveInRegion("Serengeti")).thenReturn(List.of(same));
        when(seasons.resolveAccommodationSeason(eq(10L), any())).thenReturn(season(7L));
        when(rates.findByAccommodationIdAndSeasonIdAndIsActiveTrue(10L, 7L))
            .thenReturn(List.of(rate(900L, same)));

        List<PortalCatalogueService.Option> options = service.optionsFor(quote, booked);

        assertEquals(1, options.size());
        assertTrue(options.get(0).current());
    }

    // ------------------------------------------------------------------ finding one

    /** Two neighbours plus the one they are in, so a search has something to narrow. */
    private void threeInTheRegion() {
        Accommodation farm = lodge(11L, "Farm of Dreams Lodge", "Serengeti");
        Accommodation kati = lodge(12L, "Kati Kati Camp", "Serengeti");
        Accommodation kudu = booked.getAccommodation();

        when(accommodations.findLiveInRegion("Serengeti")).thenReturn(List.of(kudu, farm, kati));
        for (Accommodation a : List.of(kudu, farm, kati)) {
            when(seasons.resolveAccommodationSeason(eq(a.getId()), any())).thenReturn(season(7L));
            when(rates.findByAccommodationIdAndSeasonIdAndIsActiveTrue(a.getId(), 7L))
                .thenReturn(List.of(rate(900L + a.getId(), a)));
        }
    }

    /**
     * Narrowing happens before the cap, not after.
     *
     * <p>The list stops at a fixed number of options. Filtering the page's copy of it would search
     * only what the cap let through, so a lodge the client can name by heart would be missing from
     * a busy region — and they would conclude we do not work with it, which is the opposite of
     * what the catalogue is for.
     */
    @Test
    @DisplayName("a search narrows by name, and is matched before the list is capped")
    void aSearchNarrowsByName() {
        threeInTheRegion();

        List<PortalCatalogueService.Option> all = service.optionsFor(quote, booked, null);
        assertTrue(all.size() > 1, "the fixture must offer more than one for this to mean anything");

        List<PortalCatalogueService.Option> found = service.optionsFor(quote, booked, "farm");
        assertTrue(found.size() < all.size(), "nothing was narrowed");
        assertTrue(found.stream().allMatch(o -> o.accommodation().toLowerCase().contains("farm")
                || o.current()),
            "only what they asked for, plus the one they already have");
    }

    @Test
    @DisplayName("the lodge they already have survives any search, so the page can still mark it")
    void theCurrentOneIsNeverFilteredOut() {
        threeInTheRegion();

        List<PortalCatalogueService.Option> found =
            service.optionsFor(quote, booked, "nothing-matches-this");

        assertTrue(found.stream().anyMatch(PortalCatalogueService.Option::current),
            "a list that drops the current choice reads as though we have taken it away");
    }

    @Test
    @DisplayName("a lodge with no photograph is still offered")
    void aMissingPhotographIsNotAMissingOption() {
        threeInTheRegion();

        List<PortalCatalogueService.Option> options = service.optionsFor(quote, booked, null);

        assertFalse(options.isEmpty());
        assertTrue(options.stream().allMatch(o -> o.imageUrl() == null),
            "the fixture holds no images; a null here must be a plain card, never a dropped row");
    }

    // ------------------------------------------------------------------ no figures

    @Test
    @DisplayName("an option carries no money of any kind, published or otherwise")
    void noFigureCrossesTheBoundary() {
        for (RecordComponent component : PortalCatalogueService.Option.class.getRecordComponents()) {
            assertFalse(Number.class.isAssignableFrom(component.getType())
                    || component.getType() == BigDecimal.class,
                component.getName() + " is a figure, and the rack rate is not what this client "
                    + "would pay: commission and uplift are applied per quotation. A number "
                    + "wrong in a way the reader cannot detect is worse than no number.");
        }
    }

    @Test
    @DisplayName("the catalogue never reads a rate's own figures")
    void theCatalogueDoesNotTouchTheRates() throws IOException {
        String source = Files.readString(Path.of("src/main/java/com/itineraryledger/"
            + "kabengosafaris/ClientPortal/Services/PortalCatalogueService.java"));
        int body = source.indexOf("public class PortalCatalogueService");

        for (String figure : new String[] {"getRackRate", "getStoRate", "getCurrency"}) {
            assertFalse(source.substring(body).contains(figure),
                figure + " has no business in a list a client reads");
        }
    }
}
