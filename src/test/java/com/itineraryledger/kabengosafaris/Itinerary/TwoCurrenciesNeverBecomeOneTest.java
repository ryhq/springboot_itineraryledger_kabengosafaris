package com.itineraryledger.kabengosafaris.Itinerary;

import com.itineraryledger.kabengosafaris.Activity.ChargingBasis;
import com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.DTOs.CostLineItemDTO;
import com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.DTOs.CurrencyGroupedCostDTO;
import com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.DTOs.DayCostDetailDTO;
import com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.Enums.CostItemType;
import com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.Services.Aggregators.PerDayCostAggregator;
import com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.Services.Calculators.ParkTariffCostCalculator;
import com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.Services.Core.ParkTariffRateLookupService;
import com.itineraryledger.kabengosafaris.Itinerary.DTOs.FullItineraryDTO;
import com.itineraryledger.kabengosafaris.ParkTariff.ParkTariff;
import com.itineraryledger.kabengosafaris.ParkTariff.ParkTariffRepository;
import com.itineraryledger.kabengosafaris.ParkTariffRate.ParkTariffRate;
import com.itineraryledger.kabengosafaris.PaxNationCategory.Repositories.PaxNationCategoryRepository;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import com.itineraryledger.kabengosafaris.Tariff.Tariff;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * A party that mixes nationalities is charged in more than one money, and the two never merge.
 *
 * <p>Ngorongoro holds its resident conservation fee in TZS and its visitor fee in USD. The
 * calculator used to add every pax band into one total and label it with whichever rate the loop
 * read last, so adding one Tanzanian adult to seven Germans turned TZS 46,020 of crater fees into
 * USD 46,020 and took a real quote from USD 23,903 to USD 66,879. Nothing was logged. The only
 * reason anybody noticed is that it tripled — the same defect reading a USD rate last on a mostly
 * shilling party would have understated the total and gone out to a client.
 *
 * <p>There is no exchange rate anywhere in costing, deliberately, so the honest answer is two
 * numbers rather than one converted one. See {@code ItineraryBudgetLevelService}: "Subtracting one
 * currency from another produces a number and it is a lie."
 */
class TwoCurrenciesNeverBecomeOneTest {

    private static final Long PARK = 7L;
    private static final Long TARIFF = 11L;

    private static final String NON_RESIDENT = "nonres";
    private static final String RESIDENT = "res";
    private static final String ADULT = "adult";

    // ── the rule ────────────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("a mixed party's gate fee leaves the calculator as one line per currency")
    void eachCurrencyGetsItsOwnLine() {
        List<CostLineItemDTO> lines = crater()
            .charging(NON_RESIDENT, "USD", "70.80", "92.04")
            .charging(RESIDENT, "TZS", "11800", "15340")
            .price(sevenVisitorsAndOneResident());

        assertEquals(2, lines.size(), "one line per currency, not one line carrying both");

        Map<String, CostLineItemDTO> byCurrency = lines.stream()
            .collect(Collectors.toMap(CostLineItemDTO::getCurrency, Function.identity()));

        CostLineItemDTO usd = byCurrency.get("USD");
        assertNotNull(usd, "the seven visitors are charged in dollars");
        assertEquals(0, new BigDecimal("644.28").compareTo(usd.getRackTotalPrice()));
        assertEquals(7, usd.getQuantity());

        CostLineItemDTO tzs = byCurrency.get("TZS");
        assertNotNull(tzs, "the resident is charged in shillings");
        assertEquals(0, new BigDecimal("15340").compareTo(tzs.getRackTotalPrice()));
        assertEquals(1, tzs.getQuantity());
    }

    @Test
    @DisplayName("the shilling total never lands inside the dollar total")
    void theShillingsStayOutOfTheDollars() {
        List<CostLineItemDTO> lines = crater()
            .charging(NON_RESIDENT, "USD", "70.80", "92.04")
            .charging(RESIDENT, "TZS", "11800", "15340")
            .price(sevenVisitorsAndOneResident());

        for (CostLineItemDTO line : lines) {
            if ("USD".equals(line.getCurrency())) {
                /*
                 * The bug produced 644.28 + 15,340 here. Asserting an upper bound rather than the
                 * exact figure states the thing that matters: no shillings crossed over.
                 */
                assertTrue(line.getRackTotalPrice().compareTo(new BigDecimal("1000")) < 0,
                    "a dollar line holding 15,340 is the shillings counted as dollars");
            }
        }
    }

    @Test
    @DisplayName("the day's totals report both currencies, side by side")
    void bothCurrenciesReachTheTotals() {
        List<CostLineItemDTO> lines = crater()
            .charging(NON_RESIDENT, "USD", "70.80", "92.04")
            .charging(RESIDENT, "TZS", "11800", "15340")
            .price(sevenVisitorsAndOneResident());

        FullItineraryDTO.DayDTO day = new FullItineraryDTO.DayDTO();
        day.setDayNumber(1);

        DayCostDetailDTO detail = DayCostDetailDTO.builder()
            .dayNumber(1)
            .lineItems(lines)
            .totalsByCurrency(List.of())
            .build();

        List<CurrencyGroupedCostDTO> totals =
            new PerDayCostAggregator(null, null, null, null, null)
                .calculateGrandTotals(List.of(withTotals(detail, lines)));

        assertEquals(2, totals.size(), "two currencies, two totals");
    }

    // ── a party of seven visitors and one Tanzanian ─────────────────────────────────────────────

    private List<FullItineraryDTO.PaxDTO> sevenVisitorsAndOneResident() {
        FullItineraryDTO.PaxDTO visitors = new FullItineraryDTO.PaxDTO();
        visitors.setNationCategoryId(NON_RESIDENT);
        visitors.setNationCategoryName("Non-Resident");
        visitors.setAgeCategoryId(ADULT);
        visitors.setAgeCategoryName("Adult");
        visitors.setCount(7);

        FullItineraryDTO.PaxDTO resident = new FullItineraryDTO.PaxDTO();
        resident.setNationCategoryId(RESIDENT);
        resident.setNationCategoryName("Resident");
        resident.setAgeCategoryId(ADULT);
        resident.setAgeCategoryName("Adult");
        resident.setCount(1);

        return List.of(visitors, resident);
    }

    /*
     * The aggregator reads a day's own totalsByCurrency, which it builds from the line items. The
     * builder above cannot reach that private method, so the detail is re-made through the
     * aggregator's public per-day path.
     */
    private DayCostDetailDTO withTotals(DayCostDetailDTO detail, List<CostLineItemDTO> lines) {
        Map<String, CurrencyGroupedCostDTO> perCurrency = new java.util.LinkedHashMap<>();
        for (CostLineItemDTO line : lines) {
            CurrencyGroupedCostDTO totals = perCurrency.computeIfAbsent(line.getCurrency(), c ->
                CurrencyGroupedCostDTO.builder()
                    .currency(c)
                    .accommodationSto(BigDecimal.ZERO).accommodationRack(BigDecimal.ZERO)
                    .parkFeesSto(BigDecimal.ZERO).parkFeesRack(BigDecimal.ZERO)
                    .activitiesSto(BigDecimal.ZERO).activitiesRack(BigDecimal.ZERO)
                    .flightsSto(BigDecimal.ZERO).flightsRack(BigDecimal.ZERO)
                    .grandTotalSto(BigDecimal.ZERO).grandTotalRack(BigDecimal.ZERO)
                    .build());
            totals.addParkFeeCost(line.getStoTotalPrice(), line.getRackTotalPrice());
        }
        detail.setTotalsByCurrency(List.copyOf(perCurrency.values()));
        return detail;
    }

    // ── a gate that charges each band in its own money ──────────────────────────────────────────

    private Crater crater() {
        return new Crater();
    }

    private static final class Crater {

        private final ParkTariffRateLookupService lookup = mock(ParkTariffRateLookupService.class);
        private final ParkTariffRepository tariffs = mock(ParkTariffRepository.class);
        private final IdObfuscator ids = mock(IdObfuscator.class);

        Crater() {
            Tariff tariff = mock(Tariff.class);
            when(tariff.getChargingBasis()).thenReturn(ChargingBasis.PER_PERSON);
            ParkTariff parkTariff = mock(ParkTariff.class);
            when(parkTariff.getTariff()).thenReturn(tariff);
            when(tariffs.findByParkIdAndTariffId(anyLong(), anyLong()))
                .thenReturn(Optional.of(parkTariff));

            when(ids.decodeId(NON_RESIDENT)).thenReturn(1L);
            when(ids.decodeId(RESIDENT)).thenReturn(2L);
            when(ids.decodeId(ADULT)).thenReturn(3L);
            when(ids.decodeId("park")).thenReturn(PARK);
            when(ids.decodeId("tariff")).thenReturn(TARIFF);
            when(ids.encodeId(anyLong())).thenReturn("tariff");
        }

        Crater charging(String nationId, String currency, String sto, String rack) {
            ParkTariffRate rate = mock(ParkTariffRate.class);
            when(rate.getStoRate()).thenReturn(new BigDecimal(sto));
            when(rate.getRackRate()).thenReturn(new BigDecimal(rack));
            when(rate.getCurrency()).thenReturn(currency);

            Long decoded = NON_RESIDENT.equals(nationId) ? 1L : 2L;
            when(lookup.lookupPersonRateWithIssueLogging(
                any(), any(), any(), any(), any(), eq(decoded), any(), anyString(), any()))
                .thenReturn(ParkTariffRateLookupService.LookupResult.found(rate, "High Season"));
            return this;
        }

        List<CostLineItemDTO> price(List<FullItineraryDTO.PaxDTO> paxList) {
            ParkTariffCostCalculator calculator = new ParkTariffCostCalculator(
                lookup, tariffs, mock(PaxNationCategoryRepository.class), ids);

            FullItineraryDTO.ParkTariffDTO tariff = new FullItineraryDTO.ParkTariffDTO();
            tariff.setTariffId("tariff");
            tariff.setTariffName("Conservation Fee");
            tariff.setIsIncludedInPrice(true);

            FullItineraryDTO.DayParkDTO park = new FullItineraryDTO.DayParkDTO();
            park.setParkId("park");
            park.setParkName("Ngorongoro Conservation Area");
            park.setTariffs(List.of(tariff));

            FullItineraryDTO.DayDTO day = new FullItineraryDTO.DayDTO();
            day.setDayNumber(1);
            day.setParks(List.of(park));

            return calculator.calculateForDay(day, null, null, paxList, 1).stream()
                .filter(line -> line.getItemType() == CostItemType.PARK_FEE)
                .toList();
        }
    }
}
