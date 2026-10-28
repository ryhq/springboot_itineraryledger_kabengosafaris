package com.itineraryledger.kabengosafaris.ClientPortal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.itineraryledger.kabengosafaris.Accommodation.Entities.Accommodation;
import com.itineraryledger.kabengosafaris.Activity.Activity;
import com.itineraryledger.kabengosafaris.ClientPortal.DTOs.PortalOutcome;
import com.itineraryledger.kabengosafaris.ClientPortal.Entity.PortalJourney;
import com.itineraryledger.kabengosafaris.ClientPortal.Entity.PortalParticipant;
import com.itineraryledger.kabengosafaris.ClientPortal.Enums.EditingLevel;
import com.itineraryledger.kabengosafaris.ClientPortal.Enums.JourneyStatus;
import com.itineraryledger.kabengosafaris.ClientPortal.Enums.ProposalState;
import com.itineraryledger.kabengosafaris.ClientPortal.Services.PortalActivityService;
import com.itineraryledger.kabengosafaris.ClientPortal.Services.PortalEditingService;
import com.itineraryledger.kabengosafaris.ClientPortal.Repository.PortalAccessEventRepository;
import com.itineraryledger.kabengosafaris.ClientPortal.Repository.PortalCodeRepository;
import com.itineraryledger.kabengosafaris.ClientPortal.Repository.PortalDeviceRepository;
import com.itineraryledger.kabengosafaris.ClientPortal.Repository.PortalJourneyRepository;
import com.itineraryledger.kabengosafaris.ClientPortal.Repository.PortalParticipantRepository;
import com.itineraryledger.kabengosafaris.ClientPortal.Services.PortalJourneyService;
import com.itineraryledger.kabengosafaris.ClientPortal.Services.PortalTokens;
import com.itineraryledger.kabengosafaris.CompanyProfile.Entity.CompanyProfile;
import com.itineraryledger.kabengosafaris.CompanyProfile.Repository.CompanyProfileRepository;
import com.itineraryledger.kabengosafaris.Itinerary.Entity.Itinerary;
import com.itineraryledger.kabengosafaris.Quote.Services.QuoteServices.QuoteRevisionService;
import com.itineraryledger.kabengosafaris.Quote.Entity.Quote;
import com.itineraryledger.kabengosafaris.Quote.QuoteDay.Entity.QuoteDay;
import com.itineraryledger.kabengosafaris.Quote.QuoteDay.QuoteDayAccommodation.Entity.QuoteDayAccommodation;
import com.itineraryledger.kabengosafaris.Quote.QuoteDay.QuoteDayAccommodation.Repository.QuoteDayAccommodationRepository;
import com.itineraryledger.kabengosafaris.Quote.QuoteDay.QuoteDayActivity.Entity.QuoteDayActivity;
import com.itineraryledger.kabengosafaris.Quote.QuoteDay.QuoteDayActivity.Repository.QuoteDayActivityRepository;
import com.itineraryledger.kabengosafaris.Quote.QuotePax.Repository.QuotePaxRepository;
import com.itineraryledger.kabengosafaris.Quote.Services.QuoteServices.QuoteCostEstimationService;
import com.itineraryledger.kabengosafaris.Quote.Services.QuoteServices.QuoteEditPolicy;

/**
 * A client reaches exactly as far as their level, and not one row further.
 *
 * <p>Three gates stand between a request and a changed quotation, and each is here because
 * each alone is insufficient. The <b>policy</b> says whether this quotation may change at all,
 * and is shared with the panel so a client and a consultant cannot be told different things.
 * The <b>level</b> says how far this client may reach. <b>Ownership</b> says whether the row
 * they named is theirs.
 *
 * <p>The third is the one that actually goes wrong in portals, and it goes wrong in exactly one
 * way: everything else is right, and a single endpoint takes an id out of a request body and
 * believes it. So the test for it is not "an id from another booking is refused" as a nicety.
 * It is the test that matters most in this file.
 */
class AClientReachesOnlyAsFarAsTheirLevelTest {

    private QuoteEditPolicy policy;
    private QuoteDayAccommodationRepository accommodations;
    private QuoteDayActivityRepository activities;
    private QuotePaxRepository party;
    private QuoteCostEstimationService costEstimation;
    private PortalJourneyService journeys;
    private PortalEditingService service;

    private Quote theirQuote;

    @BeforeEach
    void setUp() {
        policy = mock(QuoteEditPolicy.class);
        accommodations = mock(QuoteDayAccommodationRepository.class);
        activities = mock(QuoteDayActivityRepository.class);
        party = mock(QuotePaxRepository.class);
        costEstimation = mock(QuoteCostEstimationService.class);
        journeys = mock(PortalJourneyService.class);

        when(policy.canEditDayTree(any(Quote.class), any()))
            .thenReturn(QuoteEditPolicy.Decision.allow());
        when(journeys.levelOf(any())).thenReturn(EditingLevel.CHOOSE_OFFERED);

        theirQuote = new Quote();
        theirQuote.setId(1L);

        service = new PortalEditingService(policy, accommodations, activities, party,
            costEstimation, journeys, mock(PortalActivityService.class));
    }

    // ------------------------------------------------------------------ fixtures

    private PortalJourney journey() {
        PortalJourney journey = PortalJourney.builder()
            .quote(theirQuote)
            .status(JourneyStatus.OPEN)
            .proposalState(ProposalState.OPEN_TO_CLIENT)
            .locked(false)
            .build();
        journey.setId(7L);
        return journey;
    }

    private PortalParticipant lead() {
        return PortalParticipant.builder().email("sarah@example.com").build();
    }

    private QuoteDay dayOf(Quote quote) {
        QuoteDay day = new QuoteDay();
        day.setId(3L);
        day.setQuote(quote);
        return day;
    }

    private QuoteDayAccommodation stay(Long id, Quote quote, String name, boolean alternative) {
        Accommodation lodge = new Accommodation();
        lodge.setName(name);

        QuoteDayAccommodation row = new QuoteDayAccommodation();
        row.setId(id);
        row.setQuoteDay(dayOf(quote));
        row.setAccommodation(lodge);
        row.setIsAlternative(alternative);
        row.setRoomCount(1);
        return row;
    }

    private QuoteDayActivity activity(Long id, Quote quote, boolean optional) {
        Activity named = new Activity();
        named.setName("Hot air balloon");

        QuoteDayActivity row = new QuoteDayActivity();
        row.setId(id);
        row.setQuoteDay(dayOf(quote));
        row.setActivity(named);
        row.setIsOptional(optional);
        row.setIsIncludedInPrice(false);
        return row;
    }

    // ------------------------------------------------------------------ ownership

    @Test
    @DisplayName("a row on somebody else's booking is refused, and the id in the body is not a claim")
    void anIdFromAnotherBookingIsRefused() {
        Quote somebodyElses = new Quote();
        somebodyElses.setId(99L);

        when(accommodations.findById(50L))
            .thenReturn(Optional.of(stay(50L, somebodyElses, "Kudu Lodge", true)));

        PortalOutcome outcome = service.chooseStay(journey(), lead(), 50L);

        assertFalse(outcome.done(), "this is the one that actually goes wrong in portals");
        assertEquals("NOT_ON_THIS_BOOKING", outcome.code());
        verify(accommodations, never()).save(any());
    }

    @Test
    @DisplayName("a row that does not exist gets the same answer, so neither reveals the other")
    void aRowThatDoesNotExistLooksTheSame() {
        when(accommodations.findById(anyLong())).thenReturn(Optional.empty());

        PortalOutcome outcome = service.chooseStay(journey(), lead(), 50L);

        assertEquals("NOT_ON_THIS_BOOKING", outcome.code(),
            "a different answer here is a way of discovering which ids are real");
    }

    // ------------------------------------------------------------------ the level

    @Test
    @DisplayName("at Respond only, nothing changes, and the refusal tells them what to do instead")
    void respondOnlyChangesNothing() {
        when(journeys.levelOf(any())).thenReturn(EditingLevel.RESPOND);
        when(accommodations.findById(50L))
            .thenReturn(Optional.of(stay(50L, theirQuote, "Kudu Lodge", true)));

        PortalOutcome outcome = service.chooseStay(journey(), lead(), 50L);

        assertFalse(outcome.done());
        assertEquals("LEVEL_RESPOND_ONLY", outcome.code());
        assertTrue(outcome.message().contains("Tell us"),
            "a refusal with nothing to do about it is the telephone call this feature exists to avoid");
        verify(accommodations, never()).save(any());
    }

    // ------------------------------------------------------------------ the policy

    @Test
    @DisplayName("the policy's refusal is passed through word for word, not reworded here")
    void thePolicyAnswersForBothSides() {
        when(policy.canEditDayTree(any(Quote.class), any())).thenReturn(
            QuoteEditPolicy.Decision.refuse("This quotation is frozen while it is looked at.",
                "QUOTE_FROZEN"));
        when(accommodations.findById(50L))
            .thenReturn(Optional.of(stay(50L, theirQuote, "Kudu Lodge", true)));

        PortalOutcome outcome = service.chooseStay(journey(), lead(), 50L);

        assertEquals("QUOTE_FROZEN", outcome.code());
        assertEquals("This quotation is frozen while it is looked at.", outcome.message(),
            "the panel and the portal must not have two wordings for one rule");
    }

    // ------------------------------------------------------------------ swapping

    @Test
    @DisplayName("choosing an alternative swaps the two rows rather than deleting either")
    void choosingAnAlternativeSwaps() {
        QuoteDayAccommodation booked = stay(50L, theirQuote, "Kudu Lodge", false);
        QuoteDayAccommodation offered = stay(51L, theirQuote, "Farm of Dreams Lodge", true);

        when(accommodations.findById(51L)).thenReturn(Optional.of(offered));
        when(accommodations.findByQuoteDayIdOrderByIdAsc(3L))
            .thenReturn(List.of(booked, offered));

        assertTrue(service.chooseStay(journey(), lead(), 51L).done());

        assertTrue(booked.getIsAlternative(), "what they had becomes the alternative");
        assertFalse(offered.getIsAlternative(), "what they chose becomes the booking");
        verify(costEstimation).recalculate(1L);
    }

    @Test
    @DisplayName("the price is worked out again on every change, never left until later")
    void everyChangeRePrices() {
        QuoteDayAccommodation booked = stay(50L, theirQuote, "Kudu Lodge", false);
        when(accommodations.findById(50L)).thenReturn(Optional.of(booked));

        assertTrue(service.setRooms(journey(), lead(), 50L, 2).done());
        verify(costEstimation).recalculate(1L);
    }

    @Test
    @DisplayName("a room count outside what we would sell is refused rather than priced")
    void roomCountsAreBounded() {
        when(accommodations.findById(50L))
            .thenReturn(Optional.of(stay(50L, theirQuote, "Kudu Lodge", false)));

        for (Integer rooms : new Integer[] {0, -1, 500, null}) {
            PortalOutcome outcome = service.setRooms(journey(), lead(), 50L, rooms);
            assertEquals("OUT_OF_RANGE", outcome.code(), rooms + " is not a number of rooms");
        }
        verify(accommodations, never()).save(any());
    }

    // ------------------------------------------------------------------ activities

    @Test
    @DisplayName("an optional activity can be taken or left")
    void anOptionalActivityIsTheirs() {
        QuoteDayActivity balloon = activity(60L, theirQuote, true);
        when(activities.findById(60L)).thenReturn(Optional.of(balloon));

        assertTrue(service.setActivityIncluded(journey(), lead(), 60L, true).done());
        assertTrue(balloon.getIsIncludedInPrice());
    }

    @Test
    @DisplayName("an activity that is part of the trip is not a line to be deleted")
    void aCoreActivityIsNotOptional() {
        QuoteDayActivity gameDrive = activity(61L, theirQuote, false);
        when(activities.findById(61L)).thenReturn(Optional.of(gameDrive));

        PortalOutcome outcome = service.setActivityIncluded(journey(), lead(), 61L, false);

        assertFalse(outcome.done());
        assertEquals("NOT_OPTIONAL", outcome.code());
        assertTrue(outcome.message().contains("Tell us"),
            "dropping it is a different trip, not a smaller bill, so it has to be a conversation");
    }

    // ------------------------------------------------------------------ resolving the level

    /**
     * Journey, then itinerary, then company, most specific winning.
     *
     * <p>The middle tier existed only in the documentation until now, which meant the question
     * it answers had nowhere to be answered. A honeymoon built around one lodge has nothing to
     * swap and a client rearranging it produces a trip we cannot deliver; a fourteen-day circuit
     * with three alternatives a night is exactly what the portal is for. That is a fact about
     * the product, so the product is where it belongs.
     */
    private PortalJourneyService resolverWith(String companyLevel) {
        CompanyProfileRepository profiles = mock(CompanyProfileRepository.class);
        CompanyProfile profile = CompanyProfile.builder().portalEditingLevel(companyLevel).build();
        when(profiles.findAll()).thenReturn(List.of(profile));

        return new PortalJourneyService(
            mock(PortalJourneyRepository.class), mock(PortalParticipantRepository.class),
            mock(PortalDeviceRepository.class), mock(PortalCodeRepository.class),
            mock(PortalAccessEventRepository.class), mock(PortalTokens.class),
            mock(PortalActivityService.class), profiles, mock(QuoteRevisionService.class));
    }

    private PortalJourney journeyOn(EditingLevel itineraryLevel, EditingLevel journeyLevel) {
        Itinerary itinerary = new Itinerary();
        itinerary.setPortalEditingLevel(itineraryLevel);

        PortalJourney journey = journey();
        journey.setItinerary(itinerary);
        journey.setEditingLevel(journeyLevel);
        return journey;
    }

    @Test
    @DisplayName("the product's level overrides the company's")
    void theItineraryBeatsTheCompany() {
        assertEquals(EditingLevel.RESPOND,
            resolverWith("CHOOSE_CATALOGUE")
                .levelOf(journeyOn(EditingLevel.RESPOND, null)),
            "a honeymoon around one lodge has nothing to swap, whatever the company allows");
    }

    @Test
    @DisplayName("this one booking overrides the product")
    void theJourneyBeatsTheItinerary() {
        assertEquals(EditingLevel.RESPOND,
            resolverWith("CHOOSE_OFFERED")
                .levelOf(journeyOn(EditingLevel.CHOOSE_CATALOGUE, EditingLevel.RESPOND)),
            "one request that smells wrong is closed without touching the product");
    }

    @Test
    @DisplayName("a product with nothing set follows the company, which is almost every product")
    void nothingSetInherits() {
        assertEquals(EditingLevel.CHOOSE_CATALOGUE,
            resolverWith("CHOOSE_CATALOGUE").levelOf(journeyOn(null, null)));
    }

    @Test
    @DisplayName("freezing a booking beats every level, including the catalogue")
    void frozenBeatsEverything() {
        PortalJourney journey = journeyOn(EditingLevel.CHOOSE_CATALOGUE, EditingLevel.CHOOSE_CATALOGUE);
        journey.setLocked(true);

        assertEquals(EditingLevel.RESPOND, resolverWith("CHOOSE_CATALOGUE").levelOf(journey),
            "freezing is what you do when something looks wrong, so it cannot be outvoted");
    }

    // ------------------------------------------------------------------ the scan

    /**
     * A fifth edit cannot be added without the gates.
     *
     * <p>A list of the four methods would rot the moment somebody adds the fifth, and the
     * fifth is exactly the one that will be written in a hurry against a customer asking for
     * something today. Reading the source is the only check that still holds then, and the
     * failure names the method so the fix needs no detective work.
     */
    @Test
    @DisplayName("every client edit asks whether the quotation may change and whether the row is theirs")
    void noEditCanSkipTheGates() throws java.io.IOException {
        java.nio.file.Path file = java.nio.file.Path.of(
            "src/main/java/com/itineraryledger/kabengosafaris/ClientPortal/Services/"
                + "PortalEditingService.java");
        String source = java.nio.file.Files.readString(file);

        java.util.regex.Matcher m = java.util.regex.Pattern
            .compile("public PortalOutcome (\\w+)\\(PortalJourney journey, PortalParticipant")
            .matcher(source);

        List<String> offenders = new java.util.ArrayList<>();
        int found = 0;
        while (m.find()) {
            found++;
            String body = bodyAt(source, m.end());
            if (!body.contains("whyNotEditable(journey)")) offenders.add(m.group(1) + " (no gate)");
            if (!body.contains("notYours(journey)")) offenders.add(m.group(1) + " (no ownership check)");
        }

        assertEquals(4, found, "the four edits a client may make; a fifth needs a test of its own");
        assertTrue(offenders.isEmpty(),
            "these change a quotation without asking whether they may: " + offenders);
    }

    /** The source of one method, by brace matching from its signature. */
    private String bodyAt(String source, int from) {
        int open = source.indexOf('{', from);
        int depth = 0;
        for (int i = open; i < source.length(); i++) {
            char c = source.charAt(i);
            if (c == '{') depth++;
            else if (c == '}' && --depth == 0) return source.substring(open, i);
        }
        return source.substring(open);
    }
}
