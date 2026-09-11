package com.itineraryledger.kabengosafaris.Safari;

import com.itineraryledger.kabengosafaris.Accommodation.Repositories.AccommodationRateRepository;
import com.itineraryledger.kabengosafaris.Activity.ActivityRepository;
import com.itineraryledger.kabengosafaris.ActivityTariffRate.Repositories.ActivityTariffRateRepository;
import com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.Services.Core.SeasonResolverService;
import com.itineraryledger.kabengosafaris.Itinerary.DTOs.ItineraryCostEstimationDTO;
import com.itineraryledger.kabengosafaris.ParkTariff.ParkTariffRepository;
import com.itineraryledger.kabengosafaris.ParkTariffRate.Repositories.ParkTariffRateRepository;
import com.itineraryledger.kabengosafaris.PaxNationCategory.Repositories.PaxNationCategoryRepository;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Safari.DTOs.FullSafariDTO;
import com.itineraryledger.kabengosafaris.Safari.Services.SafariCostEstimationService;
import com.itineraryledger.kabengosafaris.Safari.Services.SafariFullGetService;
import com.itineraryledger.kabengosafaris.Season.Repositories.SeasonPeriodRepository;
import com.itineraryledger.kabengosafaris.Season.Season;
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
 * The same night, on the path that bills.
 *
 * <p>The engine that prices a safari is a second copy of the one that prices an itinerary, and it
 * carried the identical defect: a rate lookup that missed appended a sentence to a warnings list
 * nothing downstream reads, and the night left the invoice. Its season resolver had the same two
 * faults as well -- first period wins whether or not the season is switched on, then a fall back to
 * a GLOBAL season that can never hold an accommodation rate, which guarantees the drop.
 *
 * <p>On a quote a missing line is money not asked for and a conversation that can still be had. On
 * an invoice it is money not collected.
 */
class AnUnpricedNightStaysOnTheInvoiceTest {

    private AccommodationRateRepository rates;
    private SafariFullGetService safaris;
    private SafariCostEstimationService service;

    @BeforeEach
    void setUp() {
        rates = mock(AccommodationRateRepository.class);
        safaris = mock(SafariFullGetService.class);
        SeasonResolverService seasons = mock(SeasonResolverService.class);
        IdObfuscator ids = mock(IdObfuscator.class);
        when(ids.decodeId(anyString())).thenAnswer(inv -> Long.parseLong(inv.getArgument(0)));

        service = new SafariCostEstimationService(
            safaris,
            mock(ParkTariffRateRepository.class),
            mock(ParkTariffRepository.class),
            rates,
            mock(ActivityTariffRateRepository.class),
            mock(ActivityRepository.class),
            mock(SeasonPeriodRepository.class),
            seasons,
            mock(PaxNationCategoryRepository.class),
            ids);

        // a season resolves fine; it is the RATE that is missing
        Season season = Season.builder().name("STO 2026 - High Season").isActive(true).build();
        season.setId(7L);
        when(seasons.resolveAccommodationSeasonWithDetails(anyLong(), any()))
            .thenReturn(SeasonResolverService.AccommodationSeasonResult.found(season));
        when(rates.findActiveRate(anyLong(), anyLong(), anyLong(), anyLong(), anyLong()))
            .thenReturn(Optional.empty());
    }

    private FullSafariDTO oneNightAt(String name) {
        FullSafariDTO.DayAccommodationDTO stay = FullSafariDTO.DayAccommodationDTO.builder()
            .accommodationId("11").accommodationName(name)
            .roomTypeId("21").roomTypeName("Double Room")
            .roomStandardId("31").roomStandardName("Garden Room")
            .boardTypeId("41").boardTypeName("Half Board")
            .roomCount(2).isAlternative(false)
            .build();
        FullSafariDTO.DayDTO day = FullSafariDTO.DayDTO.builder()
            .dayNumber(1).isOvernight(true).accommodations(List.of(stay))
            .build();
        return FullSafariDTO.builder()
            .code("SAF-TEST").name("test trip").totalDays(1).totalNights(1)
            .startDate(LocalDate.of(2026, 12, 25))
            .endDate(LocalDate.of(2026, 12, 25))
            .days(List.of(day))
            .paxList(List.of(FullSafariDTO.PaxDTO.builder().count(5).build()))
            .build();
    }

    private ItineraryCostEstimationDTO estimate(FullSafariDTO dto) {
        when(safaris.getFullSafari(anyString()))
            .thenReturn(ResponseEntity.ok(ApiResponse.success(200, "ok", dto)));

        ResponseEntity<ApiResponse<?>> resp = service.estimateCosts("1", false, "USD");
        assertTrue(resp.getStatusCode().is2xxSuccessful(), "estimation should not fail outright");
        assertNotNull(resp.getBody());
        return (ItineraryCostEstimationDTO) resp.getBody().getData();
    }

    @Test
    @DisplayName("the night is still on the invoice, priced at zero and labelled")
    void theNightSurvives() {
        ItineraryCostEstimationDTO out = estimate(oneNightAt("Spice Island Hotel & Resort"));

        assertNotNull(out.getAccommodationCosts());
        assertEquals(1, out.getAccommodationCosts().getItems().size(),
            "one configured night must produce one line, priced or not");

        var line = out.getAccommodationCosts().getItems().get(0);
        assertTrue(line.getItemName().contains("Spice Island"),
            "the property has to be named, so whoever checks the invoice can spot it");
        assertEquals(0, line.getTotalPrice().signum(), "unpriced means zero, not absent");
        assertFalse(line.getRateFound());
    }

    @Test
    @DisplayName("and the estimate admits it is incomplete")
    void theEstimateSaysSo() {
        ItineraryCostEstimationDTO out = estimate(oneNightAt("Spice Island Hotel & Resort"));

        assertTrue(Boolean.TRUE.equals(out.getHasIncompleteRates()),
            "an invoice short a night must not look identical to a complete one");
        assertNotNull(out.getWarnings());
        assertTrue(out.getWarnings().stream().anyMatch(w -> w.contains("Spice Island")),
            "the warning must name what could not be priced: " + out.getWarnings());
    }

    @Test
    @DisplayName("a missing room detail does not delete the night either")
    void theOtherDropSiteIsAlsoClosed() {
        FullSafariDTO dto = oneNightAt("Spice Island Hotel & Resort");
        dto.getDays().get(0).getAccommodations().get(0).setRoomStandardId(null);

        ItineraryCostEstimationDTO out = estimate(dto);

        assertEquals(1, out.getAccommodationCosts().getItems().size(),
            "the second silent-drop path: no season or room details, line removed. Also closed.");
        assertFalse(out.getAccommodationCosts().getItems().get(0).getRateFound());
    }
}
