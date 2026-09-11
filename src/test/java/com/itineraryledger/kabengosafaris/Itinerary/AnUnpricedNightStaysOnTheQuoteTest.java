package com.itineraryledger.kabengosafaris.Itinerary;

import com.itineraryledger.kabengosafaris.Accommodation.Repositories.AccommodationRateRepository;
import com.itineraryledger.kabengosafaris.Activity.ActivityRepository;
import com.itineraryledger.kabengosafaris.ActivityTariffRate.Repositories.ActivityTariffRateRepository;
import com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.Services.Core.SeasonResolverService;
import com.itineraryledger.kabengosafaris.Itinerary.DTOs.FullItineraryDTO;
import com.itineraryledger.kabengosafaris.Itinerary.DTOs.ItineraryCostEstimationDTO;
import com.itineraryledger.kabengosafaris.Itinerary.Services.ItineraryCostEstimationService;
import com.itineraryledger.kabengosafaris.Itinerary.Services.ItineraryFullGetService;
import com.itineraryledger.kabengosafaris.ParkTariff.ParkTariffRepository;
import com.itineraryledger.kabengosafaris.ParkTariffRate.Repositories.ParkTariffRateRepository;
import com.itineraryledger.kabengosafaris.PaxNationCategory.Repositories.PaxNationCategoryRepository;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Season.Season;
import com.itineraryledger.kabengosafaris.Season.Repositories.SeasonPeriodRepository;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * A night nobody could price must still be on the quote.
 *
 * <p>It was not. When the rate lookup missed, this engine appended a sentence to a warnings list
 * that nothing downstream reads, and never added the line. The night simply left the quote.
 *
 * <p>That is how two customer quotes went out under-priced in one afternoon: each was short three
 * nights at the same Zanzibar property, 1,695 on one and 1,830 on the other, and because the
 * property was not named anywhere on the quote there was nothing to notice. A zero somebody can see
 * gets queried before a quote is sent; a line that is not there does not.
 */
class AnUnpricedNightStaysOnTheQuoteTest {

    private AccommodationRateRepository rates;
    private SeasonResolverService seasons;
    private ItineraryCostEstimationService service;

    @BeforeEach
    void setUp() {
        rates = mock(AccommodationRateRepository.class);
        seasons = mock(SeasonResolverService.class);
        IdObfuscator ids = mock(IdObfuscator.class);
        when(ids.decodeId(anyString())).thenAnswer(inv -> Long.parseLong(inv.getArgument(0)));

        service = new ItineraryCostEstimationService(
            mock(ItineraryFullGetService.class),
            mock(ParkTariffRateRepository.class),
            mock(ParkTariffRepository.class),
            rates,
            mock(ActivityTariffRateRepository.class),
            mock(ActivityRepository.class),
            mock(SeasonPeriodRepository.class),
            mock(PaxNationCategoryRepository.class),
            ids,
            seasons);

        // a season resolves fine; it is the RATE that is missing
        Season season = Season.builder().name("STO 2026 - High Season").isActive(true).build();
        season.setId(7L);
        when(seasons.resolveAccommodationSeasonWithDetails(anyLong(), any()))
            .thenReturn(SeasonResolverService.AccommodationSeasonResult.found(season));
        when(rates.findActiveRate(anyLong(), anyLong(), anyLong(), anyLong(), anyLong()))
            .thenReturn(Optional.empty());
    }

    private FullItineraryDTO oneNightAt(String name) {
        FullItineraryDTO.DayAccommodationDTO stay = FullItineraryDTO.DayAccommodationDTO.builder()
            .accommodationId("11").accommodationName(name)
            .roomTypeId("21").roomTypeName("Double Room")
            .roomStandardId("31").roomStandardName("Garden Room")
            .boardTypeId("41").boardTypeName("Half Board")
            .roomCount(2).isAlternative(false)
            .build();
        FullItineraryDTO.DayDTO day = FullItineraryDTO.DayDTO.builder()
            .dayNumber(1).isOvernight(true).accommodations(List.of(stay))
            .build();
        return FullItineraryDTO.builder()
            .code("ITI-TEST").name("test trip").totalDays(1).totalNights(1)
            .days(List.of(day))
            .paxList(List.of(FullItineraryDTO.PaxDTO.builder().count(5).build()))
            .build();
    }

    private ItineraryCostEstimationDTO estimate(FullItineraryDTO dto) {
        ResponseEntity<ApiResponse<?>> resp =
            service.estimateCostsFromDTO(dto, LocalDate.of(2026, 12, 25), false, "USD");
        assertTrue(resp.getStatusCode().is2xxSuccessful(), "estimation should not fail outright");
        assertNotNull(resp.getBody());
        return (ItineraryCostEstimationDTO) resp.getBody().getData();
    }

    @Test
    @DisplayName("the night is still there, priced at zero and labelled")
    void theNightSurvives() {
        ItineraryCostEstimationDTO out = estimate(oneNightAt("Spice Island Hotel & Resort"));

        assertNotNull(out.getAccommodationCosts());
        assertEquals(1, out.getAccommodationCosts().getItems().size(),
            "one configured night must produce one line, priced or not. Dropping it is how a quote "
            + "silently loses 1,830 and nobody sees a gap.");

        var line = out.getAccommodationCosts().getItems().get(0);
        assertTrue(line.getItemName().contains("Spice Island"),
            "the property has to be named, so somebody reading the quote can spot it");
        assertEquals(0, line.getTotalPrice().signum(), "unpriced means zero, not absent");
        assertFalse(line.getRateFound(), "this is the flag the quote writers print 'Rate not found' from");
    }

    @Test
    @DisplayName("and the estimate admits it is incomplete")
    void theEstimateSaysSo() {
        ItineraryCostEstimationDTO out = estimate(oneNightAt("Spice Island Hotel & Resort"));

        assertTrue(Boolean.TRUE.equals(out.getHasIncompleteRates()),
            "a quote that is short a night must not look identical to a complete one");
        assertNotNull(out.getWarnings());
        assertTrue(out.getWarnings().stream().anyMatch(w -> w.contains("Spice Island")),
            "the warning must name what could not be priced: " + out.getWarnings());
    }

    @Test
    @DisplayName("a missing room detail does not delete the night either")
    void theOtherDropSiteIsAlsoClosed() {
        FullItineraryDTO dto = oneNightAt("Spice Island Hotel & Resort");
        dto.getDays().get(0).getAccommodations().get(0).setRoomStandardId(null);

        ItineraryCostEstimationDTO out = estimate(dto);

        assertEquals(1, out.getAccommodationCosts().getItems().size(),
            "the second silent-drop path: no season or room details, line removed. Also closed.");
        assertFalse(out.getAccommodationCosts().getItems().get(0).getRateFound());
    }
}
